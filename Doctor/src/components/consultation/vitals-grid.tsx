import { useState } from "react";
import { Activity, Droplets, Gauge, HeartPulse, Lock, Pencil, Thermometer, Wind } from "lucide-react";

import type { Vitals } from "@/lib/healthcare-data";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

const fields = [
  { key: "bloodPressure", label: "Blood Pressure", unit: "mmHg", icon: Gauge },
  { key: "pulse", label: "Pulse Rate", unit: "bpm", icon: HeartPulse },
  { key: "temperature", label: "Temperature", unit: "°F", icon: Thermometer },
  { key: "weight", label: "Weight", unit: "kg", icon: Activity },
  { key: "height", label: "Height", unit: "cm", icon: Activity },
  { key: "bmi", label: "BMI", unit: "", icon: Activity },
  { key: "bloodSugar", label: "Blood Sugar", unit: "mg/dL", icon: Droplets },
  { key: "spo2", label: "Oxygen Saturation", unit: "%", icon: Wind },
  { key: "respiration", label: "Respiration Rate", unit: "/min", icon: Wind },
] as const;

function isAbnormal(key: string, value: string | number) {
  const n = Number(String(value).split("/")[0]);
  switch (key) {
    case "bloodPressure":
      return n >= 140;
    case "spo2":
      return n < 94;
    case "bloodSugar":
      return n >= 140;
    case "temperature":
      return n >= 99.5;
    case "pulse":
      return n > 100 || n < 55;
    case "respiration":
      return n > 20;
    default:
      return false;
  }
}

export function VitalsGrid({ vitals }: { vitals: Vitals }) {
  const [editing, setEditing] = useState(false);
  const [values, setValues] = useState<Record<string, string>>(() =>
    Object.fromEntries(fields.map((f) => [f.key, String(vitals[f.key])])),
  );

  return (
    <section className="surface-card rounded-2xl p-4 sm:p-5">
      <div className="grid grid-cols-[minmax(0,1fr)_auto] items-center gap-3 sm:flex sm:justify-between">
        <div className="min-w-0">
          <h3 className="truncate text-sm font-bold">Vitals</h3>
          <p className="truncate text-xs text-muted-foreground">
            Recorded by {vitals.recordedBy} · {vitals.recordedAt}
          </p>
        </div>
        <Button
          size="sm"
          variant={editing ? "default" : "outline"}
          className="shrink-0 rounded-xl"
          onClick={() => setEditing((e) => !e)}
        >
          {editing ? <Lock className="size-3.5" /> : <Pencil className="size-3.5" />}
          {editing ? "Lock values" : "Edit vitals"}
        </Button>
      </div>

      <div className="mt-4 grid grid-cols-2 gap-2.5 sm:grid-cols-3 xl:grid-cols-3">
        {fields.map((f) => {
          const raw = values[f.key] ?? "";
          const abnormal = isAbnormal(f.key, raw);
          return (
            <div
              key={f.key}
              className={cn(
                "min-w-0 rounded-xl border bg-card/70 p-3 transition-colors",
                abnormal && "border-destructive/35 bg-destructive/5",
              )}
            >
              <p className="flex items-center gap-1.5 truncate text-[10px] font-semibold uppercase tracking-wide text-muted-foreground">
                <f.icon className="size-3 shrink-0" />
                {f.label}
              </p>
              {editing ? (
                <Input
                  value={raw}
                  onChange={(e) => setValues((v) => ({ ...v, [f.key]: e.target.value }))}
                  className="num mt-1.5 h-8 rounded-lg text-sm font-semibold"
                />
              ) : (
                <p
                  className={cn(
                    "num mt-1 text-lg font-bold",
                    abnormal ? "text-destructive" : "text-foreground",
                  )}
                >
                  {raw}
                  <span className="ml-1 text-[11px] font-medium text-muted-foreground">{f.unit}</span>
                </p>
              )}
              {abnormal && !editing && (
                <Badge className="mt-1 rounded-full border-0 bg-destructive/12 text-[9px] text-destructive">
                  out of range
                </Badge>
              )}
            </div>
          );
        })}
      </div>
    </section>
  );
}
