import { useState } from "react";
import { AnimatePresence, motion } from "motion/react";
import {
  ChevronDown,
  FileText,
  FlaskConical,
  IndianRupee,
  ScanLine,
  Sparkles,
  Stethoscope,
} from "lucide-react";

import type { VisitRecord } from "@/lib/healthcare-data";
import { cn } from "@/lib/utils";
import { Badge } from "@/components/ui/badge";

function Block({
  icon: Icon,
  title,
  items,
}: {
  icon: typeof FileText;
  title: string;
  items: string[];
}) {
  if (!items.length) return null;
  return (
    <div className="min-w-0">
      <p className="mb-1.5 flex items-center gap-1.5 text-[10px] font-bold uppercase tracking-wide text-muted-foreground">
        <Icon className="size-3" /> {title}
      </p>
      <ul className="space-y-1">
        {items.map((i) => (
          <li key={i} className="rounded-lg bg-muted/60 px-2.5 py-1.5 text-xs font-medium">
            {i}
          </li>
        ))}
      </ul>
    </div>
  );
}

export function PatientTimeline({ visits }: { visits: VisitRecord[] }) {
  const [open, setOpen] = useState<string | null>(visits[0]?.id ?? null);

  return (
    <section className="surface-card rounded-2xl p-4 sm:p-5">
      <h3 className="text-sm font-bold">Patient Timeline</h3>
      <p className="text-xs text-muted-foreground">Longitudinal record across departments</p>

      <ol className="mt-5 space-y-3 border-l pl-5">
        {visits.map((v, i) => {
          const expanded = open === v.id;
          return (
            <li key={v.id} className="relative">
              <span
                className={cn(
                  "absolute -left-[27px] top-3 size-3 rounded-full border-2 border-background",
                  i === 0 ? "bg-primary" : "bg-muted-foreground/40",
                )}
              />
              <motion.div layout className="overflow-hidden rounded-xl border bg-card/70">
                <button
                  onClick={() => setOpen(expanded ? null : v.id)}
                  className="flex w-full items-center gap-3 px-3.5 py-3 text-left transition-colors hover:bg-primary-soft/60"
                >
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="num text-xs font-bold text-primary">{v.date}</span>
                      <Badge variant="secondary" className="rounded-full text-[10px]">
                        {v.department}
                      </Badge>
                    </div>

                    <p className="mt-1 truncate text-sm font-semibold">{v.diagnosis}</p>
                    <p className="truncate text-[11px] text-muted-foreground">{v.doctor}</p>
                  </div>
                  <ChevronDown
                    className={cn(
                      "size-4 shrink-0 text-muted-foreground transition-transform",
                      expanded && "rotate-180",
                    )}
                  />
                </button>

                <AnimatePresence initial={false}>
                  {expanded && (
                    <motion.div
                      initial={{ height: 0, opacity: 0 }}
                      animate={{ height: "auto", opacity: 1 }}
                      exit={{ height: 0, opacity: 0 }}
                      transition={{ duration: 0.25, ease: "easeOut" }}
                      className="overflow-hidden border-t"
                    >
                      <div className="space-y-3 p-3.5">
                        <div className="rounded-xl bg-primary-soft p-3">
                          <p className="mb-1 flex items-center gap-1.5 text-[10px] font-bold uppercase tracking-wide text-accent-foreground">
                            <Sparkles className="size-3" /> AI visit summary
                          </p>
                          <p className="text-xs leading-relaxed text-accent-foreground">{v.aiSummary}</p>
                        </div>

                        <div className="grid gap-3 sm:grid-cols-2">
                          <Block icon={FileText} title="Prescription" items={v.prescription} />
                          <Block icon={FlaskConical} title="Lab reports" items={v.labReports} />
                          <Block icon={ScanLine} title="Scans" items={v.scans} />
                          <Block
                            icon={Stethoscope}
                            title="Vitals"
                            items={Object.entries(v.vitals).map(([k, val]) => `${k}: ${val}`)}
                          />
                        </div>

                        <div className="flex flex-wrap items-center justify-between gap-2 text-xs">
                          <p className="min-w-0 text-muted-foreground">
                            <span className="font-semibold text-foreground">Notes: </span>
                            {v.notes}
                          </p>
                          <span className="num inline-flex shrink-0 items-center gap-1 rounded-lg bg-muted px-2 py-1 font-semibold">
                            <IndianRupee className="size-3" />
                            {v.bill.toLocaleString("en-IN")}
                          </span>
                        </div>
                      </div>
                    </motion.div>
                  )}
                </AnimatePresence>
              </motion.div>
            </li>
          );
        })}
      </ol>
    </section>
  );
}
