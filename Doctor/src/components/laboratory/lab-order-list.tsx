import { useState } from "react";
import { FlaskConical, Clock, CheckCircle2, AlertCircle, Eye, Loader2, XCircle } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { useDoctorLabOrders, LabOrderResponse, LabOrderStatus } from "@/services/lab-api";
import { LabOrderDetailsModal } from "./lab-order-details-modal";

export function LabOrderList() {
  const [page, setPage] = useState(0);
  const [selectedOrder, setSelectedOrder] = useState<LabOrderResponse | null>(null);

  const { data, isLoading, isError, error, refetch } = useDoctorLabOrders(page, 20);

  const getStatusBadge = (status: LabOrderStatus) => {
    switch (status) {
      case "ORDERED":
        return <Badge className="bg-primary/10 text-primary border-0 rounded-full text-[10px]">ORDERED</Badge>;
      case "SAMPLE_COLLECTED":
        return <Badge className="bg-info/10 text-info border-0 rounded-full text-[10px]">SAMPLE COLLECTED</Badge>;
      case "PROCESSING":
        return <Badge className="bg-warning/15 text-warning border-0 rounded-full text-[10px]">PROCESSING</Badge>;
      case "COMPLETED":
        return <Badge className="bg-success/15 text-success border-0 rounded-full text-[10px]">COMPLETED</Badge>;
      case "CANCELLED":
        return <Badge variant="secondary" className="text-muted-foreground border-0 rounded-full text-[10px]">CANCELLED</Badge>;
      default:
        return <Badge variant="outline" className="rounded-full text-[10px]">{status}</Badge>;
    }
  };

  const getPriorityBadge = (priority: string) => {
    if (priority === "STAT") return <Badge variant="destructive" className="rounded-full text-[10px]">STAT</Badge>;
    if (priority === "URGENT") return <Badge className="bg-warning text-warning-foreground border-0 rounded-full text-[10px]">URGENT</Badge>;
    return <Badge variant="outline" className="rounded-full text-[10px]">NORMAL</Badge>;
  };

  if (isLoading) {
    return (
      <div className="surface-card flex flex-col items-center justify-center rounded-2xl py-12 text-muted-foreground">
        <Loader2 className="size-6 animate-spin text-primary mb-2" />
        <p className="text-xs font-semibold">Loading doctor lab orders...</p>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="surface-card flex flex-col items-center justify-center rounded-2xl p-6 text-center text-destructive">
        <AlertCircle className="size-6 mb-2" />
        <p className="text-xs font-bold">Unable to load laboratory orders.</p>
        <p className="text-[11px] text-muted-foreground mt-1">{(error as any)?.message || "Server request failed."}</p>
        <Button size="sm" variant="outline" onClick={() => refetch()} className="mt-3 rounded-xl text-xs">
          Retry
        </Button>
      </div>
    );
  }

  const orders = data?.content || [];

  if (orders.length === 0) {
    return (
      <div className="surface-card flex flex-col items-center justify-center rounded-2xl py-12 p-6 text-center">
        <span className="grid size-12 place-items-center rounded-2xl bg-muted text-muted-foreground mb-3">
          <FlaskConical className="size-6" />
        </span>
        <p className="text-sm font-bold text-foreground">No laboratory orders found</p>
        <p className="text-xs text-muted-foreground mt-1 max-w-sm">
          Orders created by you or for your patients will appear here with real-time status updates.
        </p>
      </div>
    );
  }

  const isFirst = data ? data.number === 0 : true;
  const isLast = data ? data.number >= data.totalPages - 1 : true;

  return (
    <div className="space-y-4">
      <div className="grid gap-3 md:grid-cols-2 2xl:grid-cols-3">
        {orders.map((o) => (
          <div
            key={o.id}
            onClick={() => setSelectedOrder(o)}
            className="surface-card group cursor-pointer rounded-2xl p-4 transition-all hover:border-primary/40 hover:shadow-sm"
          >
            <div className="flex items-start justify-between gap-2">
              <div className="flex items-center gap-2.5 min-w-0">
                <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-primary/10 text-primary group-hover:bg-primary group-hover:text-primary-foreground transition-colors">
                  <FlaskConical className="size-4" />
                </span>
                <div className="min-w-0">
                  <p className="truncate text-sm font-bold text-foreground">{o.orderNumber}</p>
                  <p className="truncate text-xs text-muted-foreground">{o.patientName || `Patient #${o.patientId}`}</p>
                </div>
              </div>
              {getStatusBadge(o.status)}
            </div>

            <div className="mt-3 flex items-center justify-between border-t pt-3 text-xs">
              <div className="flex items-center gap-1.5">
                {getPriorityBadge(o.priority)}
                <span className="text-muted-foreground text-[11px]">
                  {o.items?.length || 0} test{(o.items?.length || 0) === 1 ? "" : "s"}
                </span>
              </div>
              <span className="text-[11px] text-muted-foreground">
                {new Date(o.orderedAt || o.createdAt).toLocaleDateString()}
              </span>
            </div>

            <div className="mt-2 flex items-center justify-between text-[11px] text-muted-foreground">
              <span className="truncate">
                {o.items && o.items.length > 0
                  ? o.items.map((i) => i.testNameSnapshot).join(", ")
                  : "No items"}
              </span>
              <span className="text-primary font-semibold flex items-center gap-1 shrink-0">
                View <Eye className="size-3" />
              </span>
            </div>
          </div>
        ))}
      </div>

      {/* Pagination Controls */}
      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between pt-2">
          <p className="text-xs text-muted-foreground">
            Page {data.number + 1} of {data.totalPages} ({data.totalElements} total orders)
          </p>
          <div className="flex gap-2">
            <Button
              size="sm"
              variant="outline"
              disabled={isFirst}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              className="rounded-xl text-xs"
            >
              Previous
            </Button>
            <Button
              size="sm"
              variant="outline"
              disabled={isLast}
              onClick={() => setPage((p) => p + 1)}
              className="rounded-xl text-xs"
            >
              Next
            </Button>
          </div>
        </div>
      )}

      {/* Details Modal */}
      <LabOrderDetailsModal
        order={selectedOrder}
        open={Boolean(selectedOrder)}
        onClose={() => setSelectedOrder(null)}
      />
    </div>
  );
}
