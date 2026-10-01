import React, { useEffect, useState } from "react";
import { Pill, Calendar, User, Clock, Loader2, FileText } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  prescriptionApi,
  PrescriptionResponse,
} from "@/services/prescription-api";
import { PrescriptionView } from "./prescription-view";

interface PatientPrescriptionHistoryProps {
  patientId: number;
}

export function PatientPrescriptionHistory({ patientId }: PatientPrescriptionHistoryProps) {
  const [prescriptions, setPrescriptions] = useState<PrescriptionResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedRx, setSelectedRx] = useState<PrescriptionResponse | null>(null);

  useEffect(() => {
    let isMounted = true;
    async function fetchHistory() {
      setLoading(true);
      try {
        const res = await prescriptionApi.getPatientPrescriptions(patientId, 0, 20);
        if (isMounted) {
          setPrescriptions(res.content);
        }
      } catch (err) {
        // Fallback
      } finally {
        if (isMounted) setLoading(false);
      }
    }
    fetchHistory();
    return () => {
      isMounted = false;
    };
  }, [patientId]);

  if (loading) {
    return (
      <div className="flex items-center justify-center py-8 surface-card rounded-2xl">
        <Loader2 className="size-5 animate-spin text-primary mr-2" />
        <span className="text-xs font-semibold">Loading prescription history...</span>
      </div>
    );
  }

  if (selectedRx) {
    return (
      <div className="space-y-3">
        <Button
          variant="outline"
          size="sm"
          className="rounded-xl text-xs"
          onClick={() => setSelectedRx(null)}
        >
          ← Back to History List
        </Button>
        <PrescriptionView prescription={selectedRx} />
      </div>
    );
  }

  if (prescriptions.length === 0) {
    return (
      <div className="surface-card rounded-2xl p-6 text-center text-xs text-muted-foreground">
        No previous prescriptions recorded for this patient.
      </div>
    );
  }

  return (
    <div className="surface-card rounded-2xl p-4 sm:p-5 space-y-3">
      <h3 className="text-sm font-bold flex items-center gap-2">
        <FileText className="size-4 text-primary" /> Patient Prescription History ({prescriptions.length})
      </h3>
      <div className="space-y-2.5">
        {prescriptions.map((rx) => (
          <div
            key={rx.id}
            onClick={() => setSelectedRx(rx)}
            className="rounded-xl border bg-card/70 p-3 hover:border-primary/50 cursor-pointer transition-colors flex flex-wrap items-center justify-between gap-2"
          >
            <div className="min-w-0">
              <div className="flex items-center gap-2">
                <span className="font-bold text-xs">{rx.prescriptionCode}</span>
                <Badge
                  variant={
                    rx.status === "ISSUED"
                      ? "default"
                      : rx.status === "CANCELLED"
                      ? "destructive"
                      : "secondary"
                  }
                  className="rounded-full text-[10px]"
                >
                  {rx.status}
                </Badge>
              </div>
              <p className="text-[11px] text-muted-foreground mt-0.5">
                {rx.items.length} medicine{rx.items.length === 1 ? "" : "s"} ·{" "}
                {new Date(rx.createdAt).toLocaleDateString()}
              </p>
            </div>
            <Button size="sm" variant="ghost" className="h-7 text-xs rounded-lg">
              View Details →
            </Button>
          </div>
        ))}
      </div>
    </div>
  );
}
