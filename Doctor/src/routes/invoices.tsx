import { createFileRoute } from "@tanstack/react-router";
import { useState, useEffect } from "react";
import { Receipt, Search, Filter, Loader2, ChevronRight, IndianRupee, Clock } from "lucide-react";
import { AppShell } from "@/components/layout/app-shell";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { billingApi, InvoiceResponse, InvoiceStatus } from "@/services/billing-api";
import { formatINR } from "@/components/consultation/invoice-builder";

export const Route = createFileRoute("/invoices")({
  head: () => ({
    meta: [
      { title: "Billing & Invoices — Meridian Health OS" },
      { name: "description", content: "Doctor billing dashboard and invoice management history." },
    ],
  }),
  component: DoctorInvoicesPage,
});

function DoctorInvoicesPage() {
  const [invoices, setInvoices] = useState<InvoiceResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState<string>("ALL");
  const [selectedInvoice, setSelectedInvoice] = useState<InvoiceResponse | null>(null);

  useEffect(() => {
    let isMounted = true;
    async function fetchInvoices() {
      setLoading(true);
      try {
        const res = await billingApi.getDoctorOwnInvoices(0, 50);
        if (isMounted && res.content) {
          setInvoices(res.content);
        }
      } catch {
        // Fallback silently if no invoices found
      } finally {
        if (isMounted) setLoading(false);
      }
    }

    fetchInvoices();
    return () => {
      isMounted = false;
    };
  }, []);

  const filteredInvoices = invoices.filter((inv) => {
    const matchesSearch =
      inv.invoiceNumber.toLowerCase().includes(search.toLowerCase()) ||
      (inv.notes && inv.notes.toLowerCase().includes(search.toLowerCase()));

    const matchesStatus =
      statusFilter === "ALL" || inv.status === statusFilter;

    return matchesSearch && matchesStatus;
  });

  return (
    <AppShell>
      <div className="space-y-6 p-4 sm:p-6">
        {/* Header */}
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h1 className="text-xl font-extrabold sm:text-2xl flex items-center gap-2">
              <Receipt className="size-6 text-primary" /> Billing & Invoices
            </h1>
            <p className="text-xs text-muted-foreground">
              Manage doctor invoices, track payment status, and review historical billing records.
            </p>
          </div>
        </div>

        {/* Controls */}
        <div className="flex flex-wrap items-center gap-3">
          <div className="relative flex-1 min-w-[200px]">
            <Search className="absolute left-3 top-2.5 size-4 text-muted-foreground" />
            <Input
              placeholder="Search invoice number or notes..."
              className="pl-9 rounded-xl text-xs"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>

          <div className="w-40">
            <Select value={statusFilter} onValueChange={setStatusFilter}>
              <SelectTrigger className="rounded-xl text-xs">
                <SelectValue placeholder="Filter Status" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">All Statuses</SelectItem>
                <SelectItem value="DRAFT">Draft</SelectItem>
                <SelectItem value="ISSUED">Issued</SelectItem>
                <SelectItem value="PARTIALLY_PAID">Partially Paid</SelectItem>
                <SelectItem value="PAID">Paid</SelectItem>
                <SelectItem value="CANCELLED">Cancelled</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>

        {/* Content */}
        {loading ? (
          <div className="flex items-center justify-center p-12">
            <Loader2 className="size-6 animate-spin text-primary" />
            <span className="ml-2 text-sm text-muted-foreground">Loading doctor invoices...</span>
          </div>
        ) : filteredInvoices.length === 0 ? (
          <div className="rounded-2xl border bg-card p-12 text-center shadow-sm">
            <Receipt className="mx-auto size-12 text-muted-foreground/40" />
            <h3 className="mt-3 text-sm font-bold">No Invoices Found</h3>
            <p className="mt-1 text-xs text-muted-foreground">
              No billing invoices match your query or filter criteria.
            </p>
          </div>
        ) : (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {filteredInvoices.map((inv) => (
              <div
                key={inv.id}
                className="group cursor-pointer rounded-2xl border bg-card p-5 shadow-sm transition-all hover:border-primary/50 hover:shadow-md space-y-3"
                onClick={() => setSelectedInvoice(inv)}
              >
                <div className="flex items-center justify-between">
                  <span className="font-extrabold text-sm text-foreground group-hover:text-primary transition-colors">
                    {inv.invoiceNumber}
                  </span>
                  <Badge
                    variant={
                      inv.status === "PAID"
                        ? "default"
                        : inv.status === "PARTIALLY_PAID"
                        ? "secondary"
                        : inv.status === "CANCELLED"
                        ? "destructive"
                        : "outline"
                    }
                    className="rounded-full text-[10px]"
                  >
                    {inv.status}
                  </Badge>
                </div>

                <div className="text-xs text-muted-foreground space-y-1">
                  <p>Patient ID: {inv.patientId}</p>
                  <p>Date: {new Date(inv.createdAt).toLocaleDateString()}</p>
                </div>

                <div className="pt-2 border-t flex items-center justify-between text-xs">
                  <div>
                    <span className="text-[10px] text-muted-foreground block">Total Amount</span>
                    <span className="font-bold text-sm text-foreground">{formatINR(inv.totalAmount)}</span>
                  </div>

                  <div className="text-right">
                    <span className="text-[10px] text-muted-foreground block">Due Amount</span>
                    <span className={`font-bold ${inv.dueAmount > 0 ? "text-destructive" : "text-emerald-600"}`}>
                      {formatINR(inv.dueAmount)}
                    </span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Invoice Detail Dialog */}
        <Dialog open={!!selectedInvoice} onOpenChange={() => setSelectedInvoice(null)}>
          <DialogContent className="rounded-2xl sm:max-w-lg">
            <DialogHeader>
              <DialogTitle className="flex items-center gap-2">
                <Receipt className="size-5 text-primary" /> Invoice {selectedInvoice?.invoiceNumber}
              </DialogTitle>
            </DialogHeader>

            {selectedInvoice && (
              <div className="space-y-4 py-2 text-xs">
                <div className="flex items-center justify-between border-b pb-3">
                  <div>
                    <p className="text-muted-foreground">Status:</p>
                    <Badge className="mt-1 rounded-full">{selectedInvoice.status}</Badge>
                  </div>
                  <div className="text-right">
                    <p className="text-muted-foreground">Created At:</p>
                    <p className="font-semibold">{new Date(selectedInvoice.createdAt).toLocaleString()}</p>
                  </div>
                </div>

                <div>
                  <h4 className="font-bold mb-2">Line Items</h4>
                  <div className="space-y-1 bg-muted/20 p-3 rounded-xl">
                    {selectedInvoice.items.map((item) => (
                      <div key={item.id} className="flex justify-between py-1 border-b last:border-0">
                        <span>
                          {item.description} (x{item.quantity})
                        </span>
                        <span className="font-semibold">{formatINR(item.lineTotal)}</span>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="space-y-1 bg-muted/10 p-3 rounded-xl border">
                  <div className="flex justify-between">
                    <span>Subtotal:</span>
                    <span>{formatINR(selectedInvoice.subtotal)}</span>
                  </div>
                  <div className="flex justify-between text-emerald-600">
                    <span>Discount:</span>
                    <span>- {formatINR(selectedInvoice.discount)}</span>
                  </div>
                  <div className="flex justify-between text-amber-600">
                    <span>Tax:</span>
                    <span>+ {formatINR(selectedInvoice.tax)}</span>
                  </div>
                  <div className="flex justify-between font-extrabold text-sm pt-1 border-t">
                    <span>Total Amount:</span>
                    <span className="text-primary">{formatINR(selectedInvoice.totalAmount)}</span>
                  </div>
                  <div className="flex justify-between font-bold pt-1 text-emerald-600">
                    <span>Paid Amount:</span>
                    <span>{formatINR(selectedInvoice.paidAmount)}</span>
                  </div>
                  <div className="flex justify-between font-bold text-destructive">
                    <span>Due Amount:</span>
                    <span>{formatINR(selectedInvoice.dueAmount)}</span>
                  </div>
                </div>

                {selectedInvoice.payments && selectedInvoice.payments.length > 0 && (
                  <div>
                    <h4 className="font-bold mb-2">Payment History</h4>
                    <div className="space-y-1">
                      {selectedInvoice.payments.map((p) => (
                        <div key={p.id} className="flex justify-between items-center bg-emerald-50 text-emerald-800 dark:bg-emerald-950/30 dark:text-emerald-300 p-2 rounded-lg">
                          <span>{p.paymentReference} ({p.paymentMethod})</span>
                          <span className="font-bold">{formatINR(p.amount)}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            )}
          </DialogContent>
        </Dialog>
      </div>
    </AppShell>
  );
}
