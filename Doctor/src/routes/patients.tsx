import { createFileRoute } from "@tanstack/react-router";

import { AppShell, PageHeader } from "@/components/layout/app-shell";
import { PatientSearch } from "@/components/dashboard/patient-search";
import { patients } from "@/lib/healthcare-data";
import { Badge } from "@/components/ui/badge";
import { Link } from "@tanstack/react-router";

export const Route = createFileRoute("/patients")({
  head: () => ({
    meta: [
      { title: "Patient Search — Meridian Health OS" },
      {
        name: "description",
        content:
          "Find any patient instantly by Universal Patient ID, mobile number, name, QR code or appointment number.",
      },
      { property: "og:title", content: "Patient Search — Meridian Health OS" },
      { property: "og:description", content: "Universal patient lookup across the hospital network." },
    ],
  }),
  component: PatientsPage,
});

function PatientsPage() {
  return (
    <AppShell>
      <div className="space-y-6 p-4 sm:p-6">
        <PageHeader title="Patient Search" subtitle="Universal lookup across the hospital network" />
        <PatientSearch autoFocus />
        <div>
          <h2 className="mb-3 text-sm font-bold">Recently accessed</h2>
          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
            {patients.map((p) => (
              <Link
                key={p.id}
                to="/consultation/$patientId"
                params={{ patientId: p.id }}
                className="surface-card flex items-center gap-3 rounded-2xl p-4 transition-colors hover:border-primary/40"
              >
                <img src={p.photo} alt="" className="size-12 shrink-0 rounded-xl border" />
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-semibold">{p.name}</p>
                  <p className="num truncate text-xs text-muted-foreground">{p.upid}</p>
                  <div className="mt-1.5 flex flex-wrap gap-1">
                    <Badge variant="secondary" className="rounded-full text-[10px]">
                      {p.age}y · {p.gender}
                    </Badge>
                    <Badge className="rounded-full border-0 bg-destructive/12 text-[10px] text-destructive">
                      {p.bloodGroup}
                    </Badge>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        </div>
      </div>
    </AppShell>
  );
}
