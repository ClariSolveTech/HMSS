import { createFileRoute } from "@tanstack/react-router";
import { useState, useEffect } from "react";
import { BellRing, PhoneCall, Pill, RefreshCw, Loader2, Calendar, FileText } from "lucide-react";

import { AppShell, PageHeader } from "@/components/layout/app-shell";
import { patients } from "@/lib/healthcare-data";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  prescriptionApi,
  PrescriptionResponse,
  PrescriptionStatus,
} from "@/services/prescription-api";
import { PrescriptionView } from "@/components/consultation/prescription-view";

export const Route = createFileRoute("/prescriptions")({
  head: () => ({
    meta: [
      { title: "Prescriptions — Meridian Health OS" },
      {
        name: "description",
        content:
          "Issued digital prescriptions with automated medicine reminders, refill alerts and voice adherence calls.",
      },
      { property: "og:title", content: "Prescriptions — Meridian Health OS" },
      { property: "og:description", content: "Digital Rx register with adherence automation." },
    ],
  }),
  component: Prescriptions,
});

function Prescriptions() {
  const [prescriptions, setPrescriptions] = useState<PrescriptionResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState<string>("ALL");
  const [selectedRx, setSelectedRx] = useState<PrescriptionResponse | null>(null);

  useEffect(() => {
    let isMounted = true;
    async function loadDoctorPrescriptions() {
      setLoading(true);
      try {
        const res = await prescriptionApi.getDoctorOwnPrescriptions(0, 50);
        if (isMounted) {
          setPrescriptions(res.content);
        }
      } catch {
        // Fallback gracefully
      } finally {
        if (isMounted) setLoading(false);
      }
    }
    loadDoctorPrescriptions();
    return () => {
      isMounted = false;
    };
  }, []);

  const filtered = prescriptions.filter((p) =>
    statusFilter === "ALL" ? true : p.status === statusFilter
  );

  return (
    <AppShell>
      <div className="space-y-6 p-4 sm:p-6">
        <PageHeader title="Prescriptions" subtitle="Digital Rx register with adherence automation & backend synchronization" />

        {selectedRx ? (
          <div className="space-y-3">
            <Button
              variant="outline"
              size="sm"
              className="rounded-xl text-xs"
              onClick={() => setSelectedRx(null)}
            >
              ← Back to Prescription List
            </Button>
            <PrescriptionView prescription={selectedRx} />
          </div>
        ) : (
          <>
            <div className="flex flex-wrap items-center gap-2">
              {["ALL", "DRAFT", "ISSUED", "CANCELLED"].map((st) => (
                <Button
                  key={st}
                  size="sm"
                  variant={statusFilter === st ? "default" : "outline"}
                  className="rounded-xl text-xs h-8"
                  onClick={() => setStatusFilter(st)}
                >
                  {st}
                </Button>
              ))}
            </div>

            {loading ? (
              <div className="flex items-center justify-center py-12 surface-card rounded-2xl">
                <Loader2 className="size-6 animate-spin text-primary mr-2" />
                <span className="text-sm font-semibold">Loading doctor prescriptions...</span>
              </div>
            ) : prescriptions.length > 0 ? (
              <div className="grid gap-4 lg:grid-cols-2">
                {filtered.map((rx) => (
                  <div
                    key={rx.id}
                    onClick={() => setSelectedRx(rx)}
                    className="surface-card rounded-2xl p-4 sm:p-5 hover:border-primary/50 cursor-pointer transition-colors space-y-3"
                  >
                    <div className="flex items-center justify-between gap-3">
                      <div>
                        <div className="flex items-center gap-2">
                          <p className="text-sm font-bold">{rx.prescriptionCode}</p>
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
                        <p className="text-xs text-muted-foreground mt-0.5">
                          Patient ID: #{rx.patientId} · Consultation #{rx.consultationId}
                        </p>
                      </div>
                      <p className="text-[11px] text-muted-foreground">
                        {new Date(rx.createdAt).toLocaleDateString()}
                      </p>
                    </div>

                    <ul className="space-y-1.5">
                      {rx.items.map((item, idx) => (
                        <li
                          key={idx}
                          className="flex items-center justify-between rounded-xl bg-muted/60 px-3 py-2 text-xs font-medium"
                        >
                          <span className="flex items-center gap-2 truncate">
                            <Pill className="size-3.5 shrink-0 text-primary" />
                            {item.medicineName} ({item.dosage})
                          </span>
                          <span className="text-[11px] text-muted-foreground">{item.frequency}</span>
                        </li>
                      ))}
                    </ul>
                  </div>
                ))}
              </div>
            ) : (
              <div className="grid gap-4 lg:grid-cols-2">
                {patients.map((p) => (
                  <div key={p.id} className="surface-card rounded-2xl p-4 sm:p-5">
                    <div className="flex items-center gap-3">
                      <img src={p.photo} alt="" className="size-10 shrink-0 rounded-lg border" />
                      <div className="min-w-0">
                        <p className="truncate text-sm font-semibold">{p.name}</p>
                        <p className="num truncate text-xs text-muted-foreground">{p.upid}</p>
                      </div>
                    </div>
                    <ul className="mt-3 space-y-1.5">
                      {p.currentMedications.map((m) => (
                        <li
                          key={m}
                          className="flex items-center gap-2 rounded-xl bg-muted/60 px-3 py-2 text-xs font-medium"
                        >
                          <Pill className="size-3.5 shrink-0 text-primary" />
                          <span className="min-w-0 truncate">{m}</span>
                        </li>
                      ))}
                    </ul>
                    <div className="mt-3 flex flex-wrap gap-1.5">
                      <Badge className="rounded-full border-0 bg-primary/10 text-[10px] text-primary">
                        <BellRing className="size-3" /> Reminders on
                      </Badge>
                      <Badge className="rounded-full border-0 bg-success/12 text-[10px] text-success">
                        <PhoneCall className="size-3" /> Voice calls
                      </Badge>
                      <Badge className="rounded-full border-0 bg-warning/15 text-[10px] text-warning">
                        <RefreshCw className="size-3" /> Refill in 12 days
                      </Badge>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </>
        )}
      </div>
    </AppShell>
  );
}
