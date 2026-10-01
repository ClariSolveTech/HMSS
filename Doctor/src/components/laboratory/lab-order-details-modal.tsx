import { useState } from "react";
import { FlaskConical, Clock, CheckCircle2, AlertTriangle, XCircle, FileText, User, Calendar, Loader2 } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from "@/components/ui/dialog";
import { LabOrderResponse, useCancelLabOrder, LabOrderStatus } from "@/services/lab-api";

interface LabOrderDetailsModalProps {
  order: LabOrderResponse | null;
  open: boolean;
  onClose: () => void;
}

export function LabOrderDetailsModal({ order, open, onClose }: LabOrderDetailsModalProps) {
  const [confirmCancel, setConfirmCancel] = useState(false);
  const cancelMutation = useCancelLabOrder();

  if (!order) return null;

  const canCancel = order.status === "ORDERED" || order.status === "SAMPLE_COLLECTED";

  const handleCancelOrder = async () => {
    try {
      await cancelMutation.mutateAsync(order.id);
      toast.success(`Lab Order ${order.orderNumber} has been cancelled.`);
      setConfirmCancel(false);
      onClose();
    } catch (err: any) {
      toast.error(err.message || "Failed to cancel lab order.");
    }
  };

  const getStatusBadge = (status: LabOrderStatus) => {
    switch (status) {
      case "ORDERED":
        return <Badge className="bg-primary/10 text-primary border-0">ORDERED</Badge>;
      case "SAMPLE_COLLECTED":
        return <Badge className="bg-info/10 text-info border-0">SAMPLE COLLECTED</Badge>;
      case "PROCESSING":
        return <Badge className="bg-warning/15 text-warning border-0">PROCESSING</Badge>;
      case "COMPLETED":
        return <Badge className="bg-success/15 text-success border-0">COMPLETED</Badge>;
      case "CANCELLED":
        return <Badge variant="secondary" className="text-muted-foreground border-0">CANCELLED</Badge>;
      default:
        return <Badge variant="outline">{status}</Badge>;
    }
  };

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto rounded-2xl">
        <DialogHeader>
          <div className="flex items-center justify-between border-b pb-3">
            <div className="flex items-center gap-2.5">
              <span className="grid size-9 place-items-center rounded-xl bg-primary/10 text-primary">
                <FlaskConical className="size-4" />
              </span>
              <div>
                <DialogTitle className="text-base font-extrabold">Lab Order {order.orderNumber}</DialogTitle>
                <DialogDescription className="text-xs text-muted-foreground">
                  Ordered on {new Date(order.orderedAt || order.createdAt).toLocaleString()}
                </DialogDescription>
              </div>
            </div>
            {getStatusBadge(order.status)}
          </div>
        </DialogHeader>

        <div className="space-y-4 py-2 text-xs">
          {/* Patient & Doctor Banner */}
          <div className="grid gap-3 sm:grid-cols-2 rounded-xl bg-muted/30 p-3">
            <div>
              <p className="text-muted-foreground text-[11px] font-semibold">Patient</p>
              <p className="font-bold text-foreground text-sm">{order.patientName || `Patient #${order.patientId}`}</p>
            </div>
            <div>
              <p className="text-muted-foreground text-[11px] font-semibold">Ordering Physician</p>
              <p className="font-bold text-foreground text-sm">{order.doctorName || `Doctor #${order.doctorId}`}</p>
            </div>
          </div>

          {/* Details Metadata */}
          <div className="grid gap-3 grid-cols-2 sm:grid-cols-3">
            <div className="rounded-xl border p-2.5">
              <p className="text-muted-foreground text-[10px]">Priority</p>
              <p className="font-bold text-foreground capitalize mt-0.5">{order.priority}</p>
            </div>
            <div className="rounded-xl border p-2.5">
              <p className="text-muted-foreground text-[10px]">Consultation</p>
              <p className="font-bold text-foreground mt-0.5">
                {order.consultationId ? `#${order.consultationId}` : "None"}
              </p>
            </div>
            <div className="rounded-xl border p-2.5">
              <p className="text-muted-foreground text-[10px]">Status</p>
              <p className="font-bold text-foreground mt-0.5">{order.status}</p>
            </div>
          </div>

          {/* Clinical Notes */}
          {order.clinicalNotes && (
            <div className="rounded-xl border bg-background p-3">
              <p className="text-muted-foreground font-semibold text-[11px] mb-1">Clinical Notes</p>
              <p className="text-foreground whitespace-pre-wrap">{order.clinicalNotes}</p>
            </div>
          )}

          {/* Ordered Tests List */}
          <div>
            <p className="font-bold text-foreground mb-2">Requested Laboratory Tests ({order.items?.length || 0})</p>
            <div className="space-y-2">
              {order.items?.map((item) => (
                <div key={item.id} className="flex items-center justify-between rounded-xl border p-3">
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-semibold text-foreground">{item.testNameSnapshot}</span>
                      <Badge variant="outline" className="text-[10px]">
                        {item.testCodeSnapshot}
                      </Badge>
                    </div>
                    {item.notes && <p className="text-muted-foreground text-[11px] mt-1">Note: {item.notes}</p>}
                  </div>
                  <Badge variant="secondary" className="text-[10px]">
                    {item.status}
                  </Badge>
                </div>
              ))}
            </div>
          </div>

          {/* Timestamps */}
          <div className="text-[11px] text-muted-foreground space-y-1 pt-2 border-t">
            <p>Ordered At: {new Date(order.orderedAt || order.createdAt).toLocaleString()}</p>
            {order.completedAt && <p>Completed At: {new Date(order.completedAt).toLocaleString()}</p>}
            {order.cancelledAt && <p>Cancelled At: {new Date(order.cancelledAt).toLocaleString()}</p>}
          </div>

          {/* Confirm Cancel Block */}
          {confirmCancel && (
            <div className="rounded-xl border border-destructive/30 bg-destructive/5 p-4 space-y-3">
              <div className="flex items-center gap-2 text-destructive font-bold text-xs">
                <AlertTriangle className="size-4" /> Confirm Order Cancellation
              </div>
              <p className="text-muted-foreground text-xs">
                Are you sure you want to cancel Lab Order <strong className="text-foreground">{order.orderNumber}</strong>? This action cannot be undone.
              </p>
              <div className="flex gap-2">
                <Button
                  size="sm"
                  variant="destructive"
                  disabled={cancelMutation.isPending}
                  onClick={handleCancelOrder}
                  className="rounded-xl"
                >
                  {cancelMutation.isPending ? <Loader2 className="size-3.5 animate-spin" /> : "Yes, Cancel Order"}
                </Button>
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => setConfirmCancel(false)}
                  className="rounded-xl"
                >
                  No, Keep Order
                </Button>
              </div>
            </div>
          )}
        </div>

        <DialogFooter className="border-t pt-3 flex items-center justify-between sm:justify-between">
          {canCancel && !confirmCancel ? (
            <Button
              variant="outline"
              size="sm"
              onClick={() => setConfirmCancel(true)}
              className="text-destructive hover:bg-destructive/10 rounded-xl"
            >
              Cancel Order
            </Button>
          ) : (
            <div />
          )}
          <Button variant="secondary" size="sm" onClick={onClose} className="rounded-xl">
            Close
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
