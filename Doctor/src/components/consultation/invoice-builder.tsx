import { useState, useEffect, useMemo } from "react";
import { motion, AnimatePresence } from "motion/react";
import {
  Receipt,
  Plus,
  Trash2,
  Save,
  Send,
  Ban,
  CreditCard,
  CheckCircle,
  AlertCircle,
  Loader2,
  Clock,
  IndianRupee,
  FileText,
} from "lucide-react";
import { toast } from "sonner";

import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { cn } from "@/lib/utils";
import {
  billingApi,
  InvoiceResponse,
  InvoiceItemRequest,
  InvoiceItemType,
  PaymentMethod,
  PaymentResponse,
} from "@/services/billing-api";

export function formatINR(val: number | undefined | null): string {
  if (val === undefined || val === null || isNaN(val)) return "₹0.00";
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(val);
}

interface InvoiceBuilderProps {
  consultationId?: number | undefined;
  patientId: number;
  doctorId?: number | undefined;
  appointmentId?: number | undefined;
}

export function InvoiceBuilder({
  consultationId,
  patientId,
  doctorId,
  appointmentId,
}: InvoiceBuilderProps) {
  const [invoice, setInvoice] = useState<InvoiceResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [issuing, setIssuing] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const [paying, setPaying] = useState(false);

  // Form states
  const [items, setItems] = useState<InvoiceItemRequest[]>([
    {
      itemType: "CONSULTATION",
      description: "General Consultation Fee",
      quantity: 1,
      unitPrice: 500,
      discount: 0,
      tax: 0,
    },
  ]);
  const [notes, setNotes] = useState("");

  // Dialogs
  const [showIssueModal, setShowIssueModal] = useState(false);
  const [showCancelModal, setShowCancelModal] = useState(false);
  const [showPaymentModal, setShowPaymentModal] = useState(false);

  // Payment form states
  const [paymentAmount, setPaymentAmount] = useState<string>("");
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>("CASH");
  const [txnRef, setTxnRef] = useState("");
  const [paymentNotes, setPaymentNotes] = useState("");

  // Fetch existing invoice for consultation or patient
  useEffect(() => {
    let isMounted = true;
    async function loadExistingInvoice() {
      if (!consultationId && !patientId) return;
      setLoading(true);
      try {
        const res = await billingApi.getPatientInvoices(patientId);
        if (isMounted && res.content && res.content.length > 0) {
          // Find matching consultation invoice or latest draft/issued invoice
          const match = consultationId
            ? res.content.find((inv) => inv.consultationId === consultationId)
            : res.content[0];

          if (match) {
            setInvoice(match);
            setNotes(match.notes || "");
            setItems(
              match.items.map((i) => ({
                itemType: i.itemType,
                description: i.description,
                quantity: i.quantity,
                unitPrice: i.unitPrice,
                discount: i.discount,
                tax: i.tax,
              }))
            );
          }
        }
      } catch {
        // Fallback silently if no invoices exist yet
      } finally {
        if (isMounted) setLoading(false);
      }
    }

    loadExistingInvoice();
    return () => {
      isMounted = false;
    };
  }, [consultationId, patientId]);

  // Frontend calculation previews
  const previewTotals = useMemo(() => {
    let subtotal = 0;
    let discountTotal = 0;
    let taxTotal = 0;

    items.forEach((item) => {
      const base = (item.unitPrice || 0) * (item.quantity || 1);
      subtotal += base;
      discountTotal += item.discount || 0;
      taxTotal += item.tax || 0;
    });

    const total = Math.max(0, subtotal - discountTotal + taxTotal);
    return { subtotal, discount: discountTotal, tax: taxTotal, total };
  }, [items]);

  const handleAddItem = () => {
    setItems((prev) => [
      ...prev,
      {
        itemType: "MEDICINE",
        description: "Prescribed Pharmacy Item",
        quantity: 1,
        unitPrice: 100,
        discount: 0,
        tax: 0,
      },
    ]);
  };

  const handleRemoveItem = (index: number) => {
    if (items.length <= 1) {
      toast.error("Invoice must contain at least one line item.");
      return;
    }
    setItems((prev) => prev.filter((_, i) => i !== index));
  };

  const handleItemChange = (
    index: number,
    field: keyof InvoiceItemRequest,
    val: any
  ) => {
    setItems((prev) =>
      prev.map((item, i) => (i === index ? { ...item, [field]: val } : item))
    );
  };

  const handleCreateDraft = async () => {
    if (items.length === 0) {
      toast.error("Please add at least one line item.");
      return;
    }

    setSaving(true);
    try {
      const targetDoctorId = doctorId || 1;
      const created = await billingApi.createInvoice({
        patientId,
        doctorId: targetDoctorId,
        appointmentId: appointmentId || undefined,
        consultationId: consultationId || undefined,
        notes,
        items,
      });

      setInvoice(created);
      toast.success(`Draft invoice ${created.invoiceNumber} created!`);
    } catch (err: any) {
      toast.error(err.message || "Failed to create invoice draft");
    } finally {
      setSaving(false);
    }
  };

  const handleUpdateDraft = async () => {
    if (!invoice) return;
    setSaving(true);
    try {
      const updated = await billingApi.updateInvoice(invoice.id, {
        notes,
        items,
      });
      setInvoice(updated);
      toast.success("Draft invoice changes saved!");
    } catch (err: any) {
      toast.error(err.message || "Failed to save draft invoice");
    } finally {
      setSaving(false);
    }
  };

  const handleIssueInvoice = async () => {
    if (!invoice) return;
    setIssuing(true);
    try {
      const issued = await billingApi.issueInvoice(invoice.id);
      setInvoice(issued);
      setShowIssueModal(false);
      toast.success(`Invoice ${issued.invoiceNumber} issued successfully!`);
    } catch (err: any) {
      toast.error(err.message || "Failed to issue invoice");
    } finally {
      setIssuing(false);
    }
  };

  const handleCancelInvoice = async () => {
    if (!invoice) return;
    setCancelling(true);
    try {
      const cancelled = await billingApi.cancelInvoice(invoice.id);
      setInvoice(cancelled);
      setShowCancelModal(false);
      toast.success("Invoice cancelled successfully.");
    } catch (err: any) {
      toast.error(err.message || "Failed to cancel invoice");
    } finally {
      setCancelling(false);
    }
  };

  const handleOpenPaymentModal = () => {
    if (!invoice) return;
    setPaymentAmount(invoice.dueAmount ? String(invoice.dueAmount) : "");
    setPaymentMethod("CASH");
    setTxnRef("");
    setPaymentNotes("");
    setShowPaymentModal(true);
  };

  const handleRecordPayment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!invoice) return;

    const amt = parseFloat(paymentAmount);
    if (isNaN(amt) || amt <= 0) {
      toast.error("Payment amount must be greater than 0.");
      return;
    }

    if (amt > invoice.dueAmount) {
      toast.error(
        `Payment amount (${formatINR(amt)}) exceeds due amount (${formatINR(
          invoice.dueAmount
        )}).`
      );
      return;
    }

    setPaying(true);
    try {
      await billingApi.addPayment(invoice.id, {
        amount: amt,
        paymentMethod,
        transactionReference: txnRef || undefined,
        notes: paymentNotes || undefined,
      });

      // Refresh invoice to reflect updated status and amounts
      const refreshed = await billingApi.getInvoiceById(invoice.id);
      setInvoice(refreshed);
      setShowPaymentModal(false);
      toast.success(
        `Payment of ${formatINR(amt)} recorded! Invoice is now ${refreshed.status}.`
      );
    } catch (err: any) {
      toast.error(err.message || "Failed to record payment.");
    } finally {
      setPaying(false);
    }
  };

  const isEditable = !invoice || invoice.status === "DRAFT";

  if (loading) {
    return (
      <div className="flex items-center justify-center p-12">
        <Loader2 className="size-6 animate-spin text-primary" />
        <span className="ml-2 text-sm text-muted-foreground">
          Loading billing info...
        </span>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header Banner */}
      <div className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border bg-card p-5 shadow-sm">
        <div className="flex items-center gap-3">
          <div className="grid size-10 place-items-center rounded-xl bg-primary/10 text-primary">
            <Receipt className="size-5" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h2 className="text-base font-extrabold">
                {invoice ? invoice.invoiceNumber : "New Billing Invoice"}
              </h2>
              {invoice && (
                <Badge
                  variant={
                    invoice.status === "PAID"
                      ? "default"
                      : invoice.status === "PARTIALLY_PAID"
                      ? "secondary"
                      : invoice.status === "ISSUED"
                      ? "outline"
                      : invoice.status === "CANCELLED"
                      ? "destructive"
                      : "outline"
                  }
                  className="rounded-full text-xs font-semibold"
                >
                  {invoice.status}
                </Badge>
              )}
            </div>
            <p className="text-xs text-muted-foreground">
              {invoice
                ? `Created on ${new Date(invoice.createdAt).toLocaleDateString()}`
                : "Create a formal invoice for consultation, pharmacy, and hospital services"}
            </p>
          </div>
        </div>

        {/* Action Controls */}
        <div className="flex flex-wrap items-center gap-2">
          {!invoice && (
            <Button
              size="sm"
              className="rounded-xl"
              onClick={handleCreateDraft}
              disabled={saving}
            >
              {saving ? (
                <Loader2 className="size-4 animate-spin" />
              ) : (
                <Plus className="size-4" />
              )}
              Create Invoice Draft
            </Button>
          )}

          {invoice && invoice.status === "DRAFT" && (
            <>
              <Button
                size="sm"
                variant="outline"
                className="rounded-xl"
                onClick={handleUpdateDraft}
                disabled={saving}
              >
                {saving ? (
                  <Loader2 className="size-4 animate-spin" />
                ) : (
                  <Save className="size-4" />
                )}
                Save Changes
              </Button>
              <Button
                size="sm"
                className="rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white"
                onClick={() => setShowIssueModal(true)}
                disabled={issuing}
              >
                <Send className="size-4" /> Issue Invoice
              </Button>
              <Button
                size="sm"
                variant="destructive"
                className="rounded-xl"
                onClick={() => setShowCancelModal(true)}
                disabled={cancelling}
              >
                <Ban className="size-4" /> Cancel
              </Button>
            </>
          )}

          {invoice &&
            (invoice.status === "ISSUED" ||
              invoice.status === "PARTIALLY_PAID") && (
              <Button
                size="sm"
                className="rounded-xl bg-primary text-primary-foreground"
                onClick={handleOpenPaymentModal}
              >
                <CreditCard className="size-4" /> Record Payment
              </Button>
            )}
        </div>
      </div>

      {/* Invoice Items Table/List */}
      <div className="rounded-2xl border bg-card p-5 shadow-sm space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-bold">Line Items</h3>
          {isEditable && (
            <Button
              size="sm"
              variant="ghost"
              className="rounded-xl text-xs text-primary"
              onClick={handleAddItem}
            >
              <Plus className="size-3.5" /> Add Item
            </Button>
          )}
        </div>

        <div className="space-y-3">
          {items.map((item, idx) => (
            <div
              key={idx}
              className="grid gap-3 rounded-xl border bg-muted/20 p-3 sm:grid-cols-[140px_1fr_80px_100px_90px_90px_auto] items-center"
            >
              {/* Type */}
              <div>
                <Label className="text-[10px] text-muted-foreground sm:hidden">
                  Type
                </Label>
                {isEditable ? (
                  <Select
                    value={item.itemType}
                    onValueChange={(val: InvoiceItemType) =>
                      handleItemChange(idx, "itemType", val)
                    }
                  >
                    <SelectTrigger className="h-8 rounded-lg text-xs">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="CONSULTATION">Consultation</SelectItem>
                      <SelectItem value="MEDICINE">Medicine</SelectItem>
                      <SelectItem value="LAB">Lab</SelectItem>
                      <SelectItem value="PROCEDURE">Procedure</SelectItem>
                      <SelectItem value="OTHER">Other</SelectItem>
                    </SelectContent>
                  </Select>
                ) : (
                  <Badge variant="outline" className="text-[10px]">
                    {item.itemType}
                  </Badge>
                )}
              </div>

              {/* Description */}
              <div>
                <Label className="text-[10px] text-muted-foreground sm:hidden">
                  Description
                </Label>
                {isEditable ? (
                  <Input
                    className="h-8 rounded-lg text-xs"
                    value={item.description}
                    placeholder="Description"
                    onChange={(e) =>
                      handleItemChange(idx, "description", e.target.value)
                    }
                  />
                ) : (
                  <span className="text-xs font-medium">{item.description}</span>
                )}
              </div>

              {/* Quantity */}
              <div>
                <Label className="text-[10px] text-muted-foreground sm:hidden">
                  Qty
                </Label>
                {isEditable ? (
                  <Input
                    type="number"
                    min="1"
                    className="h-8 rounded-lg text-xs"
                    value={item.quantity}
                    onChange={(e) =>
                      handleItemChange(
                        idx,
                        "quantity",
                        parseInt(e.target.value) || 1
                      )
                    }
                  />
                ) : (
                  <span className="text-xs">{item.quantity}</span>
                )}
              </div>

              {/* Unit Price */}
              <div>
                <Label className="text-[10px] text-muted-foreground sm:hidden">
                  Unit Price (₹)
                </Label>
                {isEditable ? (
                  <Input
                    type="number"
                    step="0.01"
                    min="0"
                    className="h-8 rounded-lg text-xs"
                    value={item.unitPrice}
                    onChange={(e) =>
                      handleItemChange(
                        idx,
                        "unitPrice",
                        parseFloat(e.target.value) || 0
                      )
                    }
                  />
                ) : (
                  <span className="text-xs">{formatINR(item.unitPrice)}</span>
                )}
              </div>

              {/* Discount */}
              <div>
                <Label className="text-[10px] text-muted-foreground sm:hidden">
                  Discount (₹)
                </Label>
                {isEditable ? (
                  <Input
                    type="number"
                    step="0.01"
                    min="0"
                    className="h-8 rounded-lg text-xs"
                    value={item.discount || 0}
                    onChange={(e) =>
                      handleItemChange(
                        idx,
                        "discount",
                        parseFloat(e.target.value) || 0
                      )
                    }
                  />
                ) : (
                  <span className="text-xs">{formatINR(item.discount)}</span>
                )}
              </div>

              {/* Tax */}
              <div>
                <Label className="text-[10px] text-muted-foreground sm:hidden">
                  Tax (₹)
                </Label>
                {isEditable ? (
                  <Input
                    type="number"
                    step="0.01"
                    min="0"
                    className="h-8 rounded-lg text-xs"
                    value={item.tax || 0}
                    onChange={(e) =>
                      handleItemChange(
                        idx,
                        "tax",
                        parseFloat(e.target.value) || 0
                      )
                    }
                  />
                ) : (
                  <span className="text-xs">{formatINR(item.tax)}</span>
                )}
              </div>

              {/* Remove */}
              {isEditable && (
                <div className="flex justify-end">
                  <Button
                    size="icon"
                    variant="ghost"
                    className="size-7 rounded-lg text-muted-foreground hover:text-destructive"
                    onClick={() => handleRemoveItem(idx)}
                  >
                    <Trash2 className="size-3.5" />
                  </Button>
                </div>
              )}
            </div>
          ))}
        </div>

        {/* Notes */}
        <div className="pt-2">
          <Label className="text-xs font-semibold">Invoice Notes</Label>
          {isEditable ? (
            <Textarea
              rows={2}
              className="mt-1 rounded-xl text-xs"
              placeholder="Add payment terms, patient instructions, or internal notes..."
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
            />
          ) : (
            <p className="mt-1 text-xs text-muted-foreground">
              {notes || "No notes provided."}
            </p>
          )}
        </div>
      </div>

      {/* Amounts Summary */}
      <div className="grid gap-4 sm:grid-cols-2">
        <div className="rounded-2xl border bg-card p-5 shadow-sm space-y-2">
          <h4 className="text-xs font-bold text-muted-foreground uppercase tracking-wider">
            Calculation Breakdown
          </h4>
          <div className="flex justify-between text-xs py-1 border-b">
            <span>Subtotal:</span>
            <span className="font-semibold">
              {formatINR(invoice ? invoice.subtotal : previewTotals.subtotal)}
            </span>
          </div>
          <div className="flex justify-between text-xs py-1 border-b text-emerald-600">
            <span>Discount Total:</span>
            <span className="font-semibold">
              - {formatINR(invoice ? invoice.discount : previewTotals.discount)}
            </span>
          </div>
          <div className="flex justify-between text-xs py-1 border-b text-amber-600">
            <span>Tax Total:</span>
            <span className="font-semibold">
              + {formatINR(invoice ? invoice.tax : previewTotals.tax)}
            </span>
          </div>
          <div className="flex justify-between text-sm font-extrabold pt-2">
            <span>Grand Total:</span>
            <span className="text-primary">
              {formatINR(invoice ? invoice.totalAmount : previewTotals.total)}
            </span>
          </div>
        </div>

        <div className="rounded-2xl border bg-card p-5 shadow-sm space-y-2">
          <h4 className="text-xs font-bold text-muted-foreground uppercase tracking-wider">
            Payment Status
          </h4>
          <div className="flex justify-between text-xs py-1 border-b">
            <span>Total Amount:</span>
            <span className="font-semibold">
              {formatINR(invoice ? invoice.totalAmount : previewTotals.total)}
            </span>
          </div>
          <div className="flex justify-between text-xs py-1 border-b text-emerald-600">
            <span>Paid Amount:</span>
            <span className="font-semibold">
              {formatINR(invoice ? invoice.paidAmount : 0)}
            </span>
          </div>
          <div className="flex justify-between text-sm font-extrabold pt-2">
            <span>Remaining Due:</span>
            <span
              className={
                (invoice ? invoice.dueAmount : previewTotals.total) > 0
                  ? "text-destructive"
                  : "text-emerald-600"
              }
            >
              {formatINR(invoice ? invoice.dueAmount : previewTotals.total)}
            </span>
          </div>
        </div>
      </div>

      {/* Payment History List */}
      {invoice && invoice.payments && invoice.payments.length > 0 && (
        <div className="rounded-2xl border bg-card p-5 shadow-sm space-y-3">
          <h3 className="text-sm font-bold flex items-center gap-2">
            <Clock className="size-4 text-primary" /> Recorded Payments
          </h3>
          <div className="space-y-2">
            {invoice.payments.map((p) => (
              <div
                key={p.id}
                className="flex flex-wrap items-center justify-between gap-2 rounded-xl border bg-muted/10 p-3 text-xs"
              >
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-bold">{p.paymentReference}</span>
                    <Badge variant="outline" className="text-[10px]">
                      {p.paymentMethod}
                    </Badge>
                  </div>
                  {p.transactionReference && (
                    <p className="text-[11px] text-muted-foreground mt-0.5">
                      Txn Ref: {p.transactionReference}
                    </p>
                  )}
                  {p.paidAt && (
                    <p className="text-[10px] text-muted-foreground">
                      Paid: {new Date(p.paidAt).toLocaleString()}
                    </p>
                  )}
                </div>
                <div className="text-right">
                  <span className="font-bold text-emerald-600 text-sm">
                    {formatINR(p.amount)}
                  </span>
                  <Badge className="ml-2 rounded-full text-[10px] bg-emerald-600 text-white">
                    {p.status}
                  </Badge>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Confirmation Modal for Issuing */}
      <AlertDialog open={showIssueModal} onOpenChange={setShowIssueModal}>
        <AlertDialogContent className="rounded-2xl">
          <AlertDialogHeader>
            <AlertDialogTitle>Issue Invoice?</AlertDialogTitle>
            <AlertDialogDescription>
              Are you sure you want to issue this invoice? Once issued, line
              items cannot be edited or removed.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel className="rounded-xl">Cancel</AlertDialogCancel>
            <AlertDialogAction
              className="rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white"
              onClick={handleIssueInvoice}
              disabled={issuing}
            >
              {issuing ? <Loader2 className="size-4 animate-spin" /> : "Confirm Issue"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Confirmation Modal for Cancelling */}
      <AlertDialog open={showCancelModal} onOpenChange={setShowCancelModal}>
        <AlertDialogContent className="rounded-2xl">
          <AlertDialogHeader>
            <AlertDialogTitle>Cancel Invoice?</AlertDialogTitle>
            <AlertDialogDescription>
              Are you sure you want to cancel this draft invoice? Cancelled
              invoices cannot receive payments or be edited.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel className="rounded-xl">Back</AlertDialogCancel>
            <AlertDialogAction
              className="rounded-xl bg-destructive text-destructive-foreground hover:bg-destructive/90"
              onClick={handleCancelInvoice}
              disabled={cancelling}
            >
              {cancelling ? <Loader2 className="size-4 animate-spin" /> : "Confirm Cancel"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Record Payment Dialog */}
      <Dialog open={showPaymentModal} onOpenChange={setShowPaymentModal}>
        <DialogContent className="rounded-2xl sm:max-w-md">
          <form onSubmit={handleRecordPayment}>
            <DialogHeader>
              <DialogTitle className="flex items-center gap-2">
                <CreditCard className="size-5 text-primary" /> Record Payment
              </DialogTitle>
              <DialogDescription>
                Record a payment for invoice {invoice?.invoiceNumber}. Remaining
                due: {formatINR(invoice?.dueAmount)}.
              </DialogDescription>
            </DialogHeader>

            <div className="space-y-4 py-4">
              <div>
                <Label className="text-xs font-semibold">Payment Amount (₹)</Label>
                <Input
                  type="number"
                  step="0.01"
                  min="0.01"
                  max={invoice?.dueAmount}
                  required
                  className="mt-1 rounded-xl"
                  value={paymentAmount}
                  onChange={(e) => setPaymentAmount(e.target.value)}
                />
              </div>

              <div>
                <Label className="text-xs font-semibold">Payment Method</Label>
                <Select
                  value={paymentMethod}
                  onValueChange={(val: PaymentMethod) => setPaymentMethod(val)}
                >
                  <SelectTrigger className="mt-1 rounded-xl">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="CASH">Cash</SelectItem>
                    <SelectItem value="UPI">UPI</SelectItem>
                    <SelectItem value="CARD">Debit / Credit Card</SelectItem>
                    <SelectItem value="NET_BANKING">Net Banking</SelectItem>
                    <SelectItem value="OTHER">Other</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              {paymentMethod !== "CASH" && (
                <div>
                  <Label className="text-xs font-semibold">
                    Transaction Reference (Optional)
                  </Label>
                  <Input
                    className="mt-1 rounded-xl"
                    placeholder="e.g. UPI Txn ID, Card RRN"
                    value={txnRef}
                    onChange={(e) => setTxnRef(e.target.value)}
                  />
                </div>
              )}

              <div>
                <Label className="text-xs font-semibold">Payment Notes</Label>
                <Input
                  className="mt-1 rounded-xl"
                  placeholder="Optional payment comments..."
                  value={paymentNotes}
                  onChange={(e) => setPaymentNotes(e.target.value)}
                />
              </div>
            </div>

            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                className="rounded-xl"
                onClick={() => setShowPaymentModal(false)}
              >
                Cancel
              </Button>
              <Button type="submit" className="rounded-xl" disabled={paying}>
                {paying ? (
                  <Loader2 className="size-4 animate-spin" />
                ) : (
                  "Record Payment"
                )}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
