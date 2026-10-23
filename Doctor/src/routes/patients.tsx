import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { Users, Loader2 } from "lucide-react";

import { AppShell, PageHeader } from "@/components/layout/app-shell";
import { PatientSearch } from "@/components/dashboard/patient-search";
import { patients as mockPatients } from "@/lib/healthcare-data";
import { patientApi, type PatientResponse } from "@/services/patient-api";
import { Badge } from "@/components/ui/badge";

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

function formatBloodGroup(bg?: string): string {
  if (!bg) return "O+";
  return bg
    .replace("_POSITIVE", "+")
    .replace("_NEGATIVE", "-")
    .replace("POSITIVE", "+")
    .replace("NEGATIVE", "-");
}

function formatGender(gender?: string): string {
  if (!gender) return "Unknown";
  return gender.charAt(0).toUpperCase() + gender.slice(1).toLowerCase();
}

function computeAge(dob?: string): number {
  if (!dob) return 35;
  const birthDate = new Date(dob);
  if (isNaN(birthDate.getTime())) return 35;
  const age = new Date().getFullYear() - birthDate.getFullYear();
  return age > 0 ? age : 1;
}

function PatientsPage() {
  const { data: apiData, isLoading } = useQuery({
    queryKey: ["patients-directory"],
    queryFn: () => patientApi.getPatients({ size: 12 }),
  });

  const realPatients = apiData?.content || [];

  return (
    <AppShell>
      <div className="space-y-6 p-4 sm:p-6">
        <PageHeader title="Patient Search" subtitle="Universal lookup across the hospital network" />
        <PatientSearch autoFocus />
        <div>
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-sm font-bold flex items-center gap-2">
              <Users className="size-4 text-primary" />
              {realPatients.length > 0 ? "Hospital Patient Directory" : "Recently accessed"}
            </h2>
            {isLoading && <Loader2 className="size-3.5 animate-spin text-muted-foreground" />}
          </div>

          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
            {realPatients.length > 0
              ? realPatients.map((p) => {
                  const age = computeAge(p.dateOfBirth);
                  const gender = formatGender(p.gender);
                  const blood = formatBloodGroup(p.bloodGroup);
                  const fullName = `${p.firstName} ${p.lastName}`.trim();
                  const photo = `https://api.dicebear.com/9.x/notionists/svg?seed=${encodeURIComponent(
                    p.firstName || "Patient"
                  )}&backgroundColor=dbeafe,e0f2fe,ede9fe`;

                  return (
                    <Link
                      key={p.id}
                      to="/consultation/$patientId"
                      params={{ patientId: String(p.id) }}
                      className="surface-card flex items-center gap-3 rounded-2xl p-4 transition-colors hover:border-primary/40"
                    >
                      <img
                        src={photo}
                        alt={fullName}
                        className="size-12 shrink-0 rounded-xl border object-cover"
                      />
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-1.5">
                          <p className="truncate text-sm font-semibold">{fullName}</p>
                          <Badge
                            variant="outline"
                            className="text-[9px] rounded-full border-emerald-500/30 bg-emerald-500/10 text-emerald-600 px-1.5 py-0 shrink-0"
                          >
                            Live
                          </Badge>
                        </div>
                        <p className="num truncate text-xs text-muted-foreground">
                          {p.patientCode || `PAT-${p.id}`} · {p.mobileNumber}
                        </p>
                        <div className="mt-1.5 flex flex-wrap gap-1">
                          <Badge variant="secondary" className="rounded-full text-[10px]">
                            {age}y · {gender}
                          </Badge>
                          <Badge className="rounded-full border-0 bg-destructive/12 text-[10px] text-destructive">
                            {blood}
                          </Badge>
                        </div>
                      </div>
                    </Link>
                  );
                })
              : mockPatients.map((p) => (
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
