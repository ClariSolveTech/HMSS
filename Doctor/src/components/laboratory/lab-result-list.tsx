import { useState } from "react";
import { FlaskConical, AlertTriangle, CheckCircle2, AlertCircle, Eye, Loader2 } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { useDoctorLabResults, LabResultResponse, LabResultStatus } from "@/services/lab-api";
import { LabResultDetailsModal } from "./lab-result-details-modal";

export function LabResultList() {
  const [page, setPage] = useState(0);
  const [selectedResult, setSelectedResult] = useState<LabResultResponse | null>(null);

  const { data, isLoading, isError, error, refetch } = useDoctorLabResults(page, 20);

  if (isLoading) {
    return (
      <div className="surface-card flex flex-col items-center justify-center rounded-2xl py-12 text-muted-foreground">
        <Loader2 className="size-6 animate-spin text-primary mb-2" />
        <p className="text-xs font-semibold">Loading doctor lab results...</p>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="surface-card flex flex-col items-center justify-center rounded-2xl p-6 text-center text-destructive">
        <AlertCircle className="size-6 mb-2" />
        <p className="text-xs font-bold">Unable to load laboratory results.</p>
        <p className="text-[11px] text-muted-foreground mt-1">{(error as any)?.message || "Server request failed."}</p>
        <Button size="sm" variant="outline" onClick={() => refetch()} className="mt-3 rounded-xl text-xs">
          Retry
        </Button>
      </div>
    );
  }

  const results = data?.content || [];

  if (results.length === 0) {
    return (
      <div className="surface-card flex flex-col items-center justify-center rounded-2xl py-12 p-6 text-center">
        <span className="grid size-12 place-items-center rounded-2xl bg-muted text-muted-foreground mb-3">
          <FlaskConical className="size-6" />
        </span>
        <p className="text-sm font-bold text-foreground">No laboratory results available</p>
        <p className="text-xs text-muted-foreground mt-1 max-w-sm">
          Completed lab results for your orders will appear here for review and verification.
        </p>
      </div>
    );
  }

  const isFirst = data ? data.number === 0 : true;
  const isLast = data ? data.number >= data.totalPages - 1 : true;

  return (
    <div className="space-y-4">
      <div className="grid gap-3 md:grid-cols-2 2xl:grid-cols-3">
        {results.map((r) => (
          <div
            key={r.id}
            onClick={() => setSelectedResult(r)}
            className={`surface-card group cursor-pointer rounded-2xl p-4 transition-all hover:border-primary/40 hover:shadow-sm ${
              r.abnormalFlag ? "border-destructive/30 bg-destructive/5" : ""
            }`}
          >
            <div className="flex items-start justify-between gap-2">
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2">
                  <span className="font-bold text-sm text-foreground truncate">{r.testNameSnapshot}</span>
                  {r.abnormalFlag && (
                    <Badge variant="destructive" className="rounded-full text-[10px] shrink-0">
                      <AlertTriangle className="size-3 mr-0.5" /> ABNORMAL
                    </Badge>
                  )}
                </div>
                <p className="text-xs text-muted-foreground truncate">{r.patientName || `Patient #${r.patientId}`}</p>
              </div>

              <Badge
                variant={r.resultStatus === "REVIEWED" ? "default" : "secondary"}
                className={`rounded-full border-0 text-[10px] shrink-0 ${
                  r.resultStatus === "REVIEWED" ? "bg-success/15 text-success" : "bg-info/15 text-info"
                }`}
              >
                {r.resultStatus}
              </Badge>
            </div>

            <div className="mt-3 flex items-baseline justify-between">
              <div>
                <span className={`text-lg font-black ${r.abnormalFlag ? "text-destructive" : "text-foreground"}`}>
                  {r.resultValue || "—"}
                </span>
                {r.resultUnit && <span className="ml-1 text-xs text-muted-foreground">{r.resultUnit}</span>}
              </div>
              {r.referenceRange && (
                <span className="text-[10px] text-muted-foreground">
                  Ref: {r.referenceRange}
                </span>
              )}
            </div>

            <div className="mt-3 flex items-center justify-between border-t pt-2.5 text-[11px] text-muted-foreground">
              <span>{r.resultedAt ? new Date(r.resultedAt).toLocaleDateString() : "Date N/A"}</span>
              <span className="text-primary font-semibold flex items-center gap-1">
                Details <Eye className="size-3" />
              </span>
            </div>
          </div>
        ))}
      </div>

      {/* Pagination Controls */}
      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between pt-2">
          <p className="text-xs text-muted-foreground">
            Page {data.number + 1} of {data.totalPages} ({data.totalElements} total results)
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
      <LabResultDetailsModal
        result={selectedResult}
        open={Boolean(selectedResult)}
        onClose={() => setSelectedResult(null)}
      />
    </div>
  );
}
