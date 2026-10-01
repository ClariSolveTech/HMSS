import { useState, useEffect } from "react";
import { Receipt, Loader2, ChevronDown, ChevronUp } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { billingApi, InvoiceResponse } from "@/services/billing-api";
import { formatINR } from "./invoice-builder";

interface PatientBillingHistoryProps {
  patientId: number;
}

export function PatientBillingHistory({ patientId }: PatientBillingHistoryProps) {
  const [invoices, setInvoices] = useState<InvoiceResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [expandedId, setExpandedId] = useState<number | null>(null);

  useEffect(() => {
    let isMounted = true;
    async function loadInvoices() {
      if (!patientId) return;
      setLoading(true);
      try {
        const res = await billingApi.getPatientInvoices(patientId);
        if (isMounted && res.content) {
          setInvoices(res.content);
        }
      } catch {
        // Fallback silently if history unavailable
      } finally {
        if (isMounted) setLoading(false);
      }
    }

    loadInvoices();
    return () => {
      isMounted = false;
    };
  }, [patientId]);

  if (loading) {
    return (
      <div className="flex items-center justify-center p-6">
        <Loader2 className="size-5 animate-spin text-primary" />
        <span className="ml-2 text-xs text-muted-foreground">Loading patient billing history...</span>
      </div>
    );
  }

  if (invoices.length === 0) {
    return (
      <div className="rounded-2xl border bg-card p-6 text-center shadow-sm">
        <Receipt className="mx-auto size-8 text-muted-foreground/50" />
        <p className="mt-2 text-xs font-semibold text-muted-foreground">No historical billing invoices found for this patient.</p>
      </div>
    );
  }

  return (
    <div className="rounded-2xl border bg-card p-5 shadow-sm space-y-4">
      <h3 className="text-sm font-bold flex items-center gap-2">
        <Receipt className="size-4 text-primary" /> Patient Billing History ({invoices.length})
      </h3>

      <div className="space-y-3">
        {invoices.map((inv) => {
          const isExpanded = expandedId === inv.id;
          return (
            <div key={inv.id} className="rounded-xl border bg-muted/10 p-4 space-y-3">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-extrabold text-sm">{inv.invoiceNumber}</span>
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
                  <p className="text-xs text-muted-foreground">
                    Date: {new Date(inv.createdAt).toLocaleDateString()}
                  </p>
                </div>

                <div className="flex items-center gap-4">
                  <div className="text-right text-xs">
                    <p className="font-bold text-sm">{formatINR(inv.totalAmount)}</p>
                    <p className="text-[11px] text-muted-foreground">
                      Paid: {formatINR(inv.paidAmount)} · Due: {formatINR(inv.dueAmount)}
                    </p>
                  </div>

                  <Button
                    size="icon"
                    variant="ghost"
                    className="size-8 rounded-lg"
                    onClick={() => setExpandedId(isExpanded ? null : inv.id)}
                  >
                    {isExpanded ? <ChevronUp className="size-4" /> : <ChevronDown className="size-4" />}
                  </Button>
                </div>
              </div>

              {/* Expandable details */}
              {isExpanded && (
                <div className="pt-3 border-t text-xs space-y-2">
                  <div>
                    <h5 className="font-semibold text-muted-foreground">Line Items:</h5>
                    <ul className="mt-1 space-y-1">
                      {inv.items.map((i) => (
                        <li key={i.id} className="flex justify-between text-muted-foreground">
                          <span>
                            • {i.description} (x{i.quantity})
                          </span>
                          <span className="font-medium text-foreground">{formatINR(i.lineTotal)}</span>
                        </li>
                      ))}
                    </ul>
                  </div>

                  {inv.payments && inv.payments.length > 0 && (
                    <div className="pt-2">
                      <h5 className="font-semibold text-muted-foreground">Payments:</h5>
                      <ul className="mt-1 space-y-1">
                        {inv.payments.map((p) => (
                          <li key={p.id} className="flex justify-between text-emerald-600">
                            <span>
                              ✓ {p.paymentReference} ({p.paymentMethod})
                            </span>
                            <span className="font-bold">{formatINR(p.amount)}</span>
                          </li>
                        ))}
                      </ul>
                    </div>
                  )}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
