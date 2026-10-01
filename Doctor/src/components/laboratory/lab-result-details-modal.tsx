import { useState } from "react";
import { FlaskConical, AlertTriangle, CheckCircle2, FileText, User, Calendar, Loader2 } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from "@/components/ui/dialog";
import { LabResultResponse, useReviewLabResult, ReviewLabResultRequest } from "@/services/lab-api";

interface LabResultDetailsModalProps {
  result: LabResultResponse | null;
  open: boolean;
  onClose: () => void;
}

export function LabResultDetailsModal({ result, open, onClose }: LabResultDetailsModalProps) {
  const [reviewing, setReviewing] = useState(false);
  const [doctorNotes, setDoctorNotes] = useState("");
  const reviewMutation = useReviewLabResult();

  if (!result) return null;

  const isCompleted = result.resultStatus === "COMPLETED";
  const isReviewed = result.resultStatus === "REVIEWED";

  const handleReviewSubmit = async () => {
    try {
      const payload: ReviewLabResultRequest = {
        ...(doctorNotes.trim() ? { interpretation: doctorNotes.trim() } : {}),
      };
      await reviewMutation.mutateAsync({
        id: result.id,
        data: payload,
      });
      toast.success(`Lab result #${result.id} reviewed successfully!`);
      setReviewing(false);
      onClose();
    } catch (err: any) {
      toast.error(err.message || "Failed to review lab result.");
    }
  };

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-w-xl max-h-[90vh] overflow-y-auto rounded-2xl">
        <DialogHeader>
          <div className="flex items-center justify-between border-b pb-3">
            <div className="flex items-center gap-2.5">
              <span className="grid size-9 place-items-center rounded-xl bg-primary/10 text-primary">
                <FlaskConical className="size-4" />
              </span>
              <div>
                <DialogTitle className="text-base font-extrabold">{result.testNameSnapshot}</DialogTitle>
                <DialogDescription className="text-xs text-muted-foreground">
                  Code: {result.testCodeSnapshot || "N/A"} · Lab Order #{result.labOrderId}
                </DialogDescription>
              </div>
            </div>
            <Badge
              variant={isReviewed ? "default" : isCompleted ? "secondary" : "outline"}
              className={`rounded-full border-0 text-[10px] ${
                isReviewed
                  ? "bg-success/15 text-success"
                  : isCompleted
                  ? "bg-info/15 text-info"
                  : "bg-warning/15 text-warning"
              }`}
            >
              {result.resultStatus}
            </Badge>
          </div>
        </DialogHeader>

        <div className="space-y-4 py-2 text-xs">
          {/* Patient & Doctor Banner */}
          <div className="grid gap-3 sm:grid-cols-2 rounded-xl bg-muted/30 p-3">
            <div>
              <p className="text-muted-foreground text-[10px] font-semibold">Patient</p>
              <p className="font-bold text-foreground">{result.patientName || `Patient #${result.patientId}`}</p>
            </div>
            <div>
              <p className="text-muted-foreground text-[10px] font-semibold">Doctor</p>
              <p className="font-bold text-foreground">{result.doctorName || `Doctor #${result.doctorId}`}</p>
            </div>
          </div>

          {/* Test Result Display Card */}
          <div className="rounded-xl border bg-background p-4 space-y-3">
            <div className="flex items-center justify-between">
              <p className="text-xs text-muted-foreground font-semibold">Laboratory Finding</p>
              {result.abnormalFlag && (
                <Badge variant="destructive" className="rounded-full text-[10px] flex items-center gap-1">
                  <AlertTriangle className="size-3" /> ABNORMAL VALUE
                </Badge>
              )}
            </div>

            <div className="flex items-baseline gap-2">
              <span className={`text-2xl font-black ${result.abnormalFlag ? "text-destructive" : "text-foreground"}`}>
                {result.resultValue || "Pending"}
              </span>
              {result.resultUnit && <span className="text-sm font-semibold text-muted-foreground">{result.resultUnit}</span>}
            </div>

            {result.referenceRange && (
              <p className="text-xs text-muted-foreground">
                Normal Reference Range: <strong className="text-foreground">{result.referenceRange} {result.resultUnit || ""}</strong>
              </p>
            )}
          </div>

          {/* Laboratory Observations & Remarks */}
          {result.remarks && (
            <div className="rounded-xl border p-3">
              <p className="text-muted-foreground font-semibold text-[11px] mb-1">Lab Technician Remarks</p>
              <p className="text-foreground">{result.remarks}</p>
            </div>
          )}

          {/* Existing Interpretation */}
          {result.interpretation && (
            <div className="rounded-xl border bg-primary/5 p-3">
              <p className="text-primary font-semibold text-[11px] mb-1">Clinical Interpretation</p>
              <p className="text-foreground">{result.interpretation}</p>
            </div>
          )}

          {/* Timestamps & Reviewer */}
          <div className="text-[11px] text-muted-foreground space-y-1 pt-2 border-t">
            {result.resultedAt && <p>Resulted At: {new Date(result.resultedAt).toLocaleString()}</p>}
            {result.reviewedAt && (
              <p className="text-success font-semibold">
                Reviewed At: {new Date(result.reviewedAt).toLocaleString()} {result.reviewedBy ? `by ${result.reviewedBy}` : ""}
              </p>
            )}
          </div>

          {/* Review Action Form */}
          {isCompleted && (
            <div className="rounded-xl border bg-muted/20 p-4 space-y-3">
              <p className="font-bold text-xs text-foreground flex items-center gap-1.5">
                <CheckCircle2 className="size-4 text-primary" /> Doctor Review Action
              </p>

              {!reviewing ? (
                <Button
                  size="sm"
                  onClick={() => setReviewing(true)}
                  className="w-full rounded-xl font-bold"
                >
                  Review & Mark as Verified
                </Button>
              ) : (
                <div className="space-y-3">
                  <div className="space-y-1">
                    <Label className="text-xs font-semibold">Doctor Clinical Notes / Verification (Optional)</Label>
                    <Textarea
                      placeholder="Add clinical evaluation notes..."
                      value={doctorNotes}
                      onChange={(e) => setDoctorNotes(e.target.value)}
                      rows={2}
                      className="text-xs rounded-xl"
                    />
                  </div>
                  <div className="flex gap-2">
                    <Button
                      size="sm"
                      disabled={reviewMutation.isPending}
                      onClick={handleReviewSubmit}
                      className="flex-1 rounded-xl font-bold"
                    >
                      {reviewMutation.isPending ? (
                        <Loader2 className="size-3.5 animate-spin" />
                      ) : (
                        "Confirm Verification"
                      )}
                    </Button>
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => setReviewing(false)}
                      className="rounded-xl"
                    >
                      Cancel
                    </Button>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>

        <DialogFooter className="border-t pt-3">
          <Button variant="secondary" size="sm" onClick={onClose} className="rounded-xl">
            Close
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
