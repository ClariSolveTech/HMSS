import { createFileRoute, Link } from "@tanstack/react-router";
import { FileText } from "lucide-react";

import { AppShell, PageHeader } from "@/components/layout/app-shell";
import { patients } from "@/lib/healthcare-data";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";

export const Route = createFileRoute("/history")({
  head: () => ({
    meta: [
      { title: "Consultation History — Meridian Health OS" },
      {
        name: "description",
        content: "Every past consultation with diagnosis, prescriptions, labs and AI visit summaries.",
      },
      { property: "og:title", content: "Consultation History — Meridian Health OS" },
      { property: "og:description", content: "Searchable longitudinal record of all consultations." },
    ],
  }),
  component: History,
});

function History() {
  const visits = patients.flatMap((p) => p.timeline.map((v) => ({ ...v, patient: p })));
  return (
    <AppShell>
      <div className="space-y-6 p-4 sm:p-6">
        <PageHeader title="Consultation History" subtitle={`${visits.length} recorded consultations`} />
        <div className="surface-card overflow-hidden rounded-2xl">
          <ul className="divide-y">
            {visits.map((v) => (
              <li key={`${v.patient.id}-${v.id}`} className="flex flex-wrap items-center gap-3 px-4 py-3.5 hover:bg-primary-soft/50">
                <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-primary/10 text-primary">
                  <FileText className="size-4" />
                </span>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-semibold">{v.diagnosis}</p>
                  <p className="truncate text-xs text-muted-foreground">
                    {v.patient.name} · {v.doctor} · {v.department}
                  </p>
                </div>
                <Badge variant="secondary" className="num shrink-0 rounded-full text-[10px]">
                  {v.date}
                </Badge>
                <Link to="/consultation/$patientId" params={{ patientId: v.patient.id }} className="shrink-0">
                  <Button size="sm" variant="outline" className="rounded-xl">
                    View
                  </Button>
                </Link>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </AppShell>
  );
}
