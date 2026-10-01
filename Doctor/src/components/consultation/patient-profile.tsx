import {
  AlertTriangle,
  Droplet,
  HeartPulse,
  Phone,
  Ruler,
  ShieldCheck,
  Pill,
  Weight,
} from "lucide-react";

import type { Patient } from "@/lib/healthcare-data";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";

function Field({ label, value }: { label: string; value: string }) {
  return (
    <div className="min-w-0 rounded-xl bg-muted/60 px-3 py-2">
      <p className="text-[10px] font-semibold uppercase tracking-wide text-muted-foreground">
        {label}
      </p>
      <p className="num truncate text-sm font-semibold">{value}</p>
    </div>
  );
}

export function PatientProfileCard({ patient }: { patient: Patient }) {
  return (
    <div className="surface-card rounded-2xl p-4 sm:p-5">
      <div className="flex items-center gap-3">
        <img
          src={patient.photo}
          alt={patient.name}
          className="size-16 shrink-0 rounded-2xl border object-cover"
        />
        <div className="min-w-0">
          <h2 className="truncate text-lg font-bold">{patient.name}</h2>
          <p className="num truncate text-xs text-muted-foreground">{patient.upid}</p>
          <div className="mt-1.5 flex flex-wrap gap-1.5">
            <Badge variant="secondary" className="rounded-full text-[10px]">
              {patient.age}y · {patient.gender}
            </Badge>
            <Badge className="rounded-full border-0 bg-destructive/12 text-[10px] text-destructive">
              <Droplet className="size-3" /> {patient.bloodGroup}
            </Badge>
          </div>
        </div>
      </div>

      <div className="mt-4 grid grid-cols-3 gap-2">
        <Field label="Height" value={`${patient.height} cm`} />
        <Field label="Weight" value={`${patient.weight} kg`} />
        <Field label="BMI" value={`${patient.bmi}`} />
      </div>

      <Separator className="my-4" />

      <section className="space-y-3">
        <div>
          <p className="mb-1.5 flex items-center gap-1.5 text-xs font-bold uppercase tracking-wide text-destructive">
            <AlertTriangle className="size-3.5" /> Medical alerts
          </p>
          <ul className="space-y-1">
            {patient.alerts.map((a) => (
              <li
                key={a}
                className="rounded-lg bg-destructive/10 px-2.5 py-1.5 text-xs font-medium text-destructive"
              >
                {a}
              </li>
            ))}
          </ul>
        </div>

        <div>
          <p className="mb-1.5 text-xs font-bold uppercase tracking-wide text-muted-foreground">
            Allergies
          </p>
          <div className="flex flex-wrap gap-1.5">
            {patient.allergies.length ? (
              patient.allergies.map((a) => (
                <Badge key={a} className="rounded-full border-0 bg-warning/15 text-[11px] text-warning">
                  {a}
                </Badge>
              ))
            ) : (
              <span className="text-xs text-muted-foreground">No known allergies</span>
            )}
          </div>
        </div>

        <div>
          <p className="mb-1.5 flex items-center gap-1.5 text-xs font-bold uppercase tracking-wide text-muted-foreground">
            <HeartPulse className="size-3.5" /> Chronic conditions
          </p>
          <div className="flex flex-wrap gap-1.5">
            {patient.chronic.map((c) => (
              <Badge key={c} variant="secondary" className="rounded-full text-[11px]">
                {c}
              </Badge>
            ))}
          </div>
        </div>

        <div>
          <p className="mb-1.5 flex items-center gap-1.5 text-xs font-bold uppercase tracking-wide text-muted-foreground">
            <Pill className="size-3.5" /> Current medications
          </p>
          <ul className="space-y-1 text-xs">
            {patient.currentMedications.map((m) => (
              <li key={m} className="rounded-lg bg-muted/60 px-2.5 py-1.5 font-medium">
                {m}
              </li>
            ))}
          </ul>
        </div>

        <Separator />

        <div className="space-y-2 text-xs">
          <p className="flex items-start gap-2">
            <Phone className="mt-0.5 size-3.5 shrink-0 text-muted-foreground" />
            <span className="min-w-0">
              <span className="font-semibold">{patient.emergencyContact.name}</span>{" "}
              <span className="text-muted-foreground">({patient.emergencyContact.relation})</span>
              <span className="num block text-muted-foreground">{patient.emergencyContact.phone}</span>
            </span>
          </p>
          <p className="flex items-start gap-2">
            <ShieldCheck className="mt-0.5 size-3.5 shrink-0 text-muted-foreground" />
            <span className="min-w-0">
              <span className="font-semibold">{patient.insurance.provider}</span>
              <span className="num block text-muted-foreground">
                {patient.insurance.policy} · valid till {patient.insurance.validTill}
              </span>
            </span>
          </p>
          <p className="flex items-start gap-2">
            <Ruler className="mt-0.5 size-3.5 shrink-0 text-muted-foreground" />
            <span className="text-muted-foreground">
              Vitals recorded by {patient.vitals.recordedBy} at {patient.vitals.recordedAt}
            </span>
          </p>
          <p className="flex items-start gap-2">
            <Weight className="mt-0.5 size-3.5 shrink-0 text-muted-foreground" />
            <span className="text-muted-foreground">Mobile {patient.mobile}</span>
          </p>
        </div>
      </section>
    </div>
  );
}
