import { AnimatePresence, motion } from "motion/react";
import {
  Activity,
  AlertTriangle,
  Brain,
  ChevronRight,
  HeartPulse,
  Pill,
  Sparkles,
  TrendingUp,
  X,
} from "lucide-react";

import type { Patient } from "@/lib/healthcare-data";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";

function Section({
  icon: Icon,
  title,
  children,
  tone = "default",
}: {
  icon: typeof Brain;
  title: string;
  children: React.ReactNode;
  tone?: "default" | "danger";
}) {
  return (
    <div
      className={
        tone === "danger"
          ? "rounded-xl border border-destructive/30 bg-destructive/5 p-3"
          : "rounded-xl border bg-card/70 p-3"
      }
    >
      <p
        className={
          tone === "danger"
            ? "mb-1.5 flex items-center gap-1.5 text-[10px] font-bold uppercase tracking-wide text-destructive"
            : "mb-1.5 flex items-center gap-1.5 text-[10px] font-bold uppercase tracking-wide text-muted-foreground"
        }
      >
        <Icon className="size-3" /> {title}
      </p>
      <div className="space-y-1.5 text-xs leading-relaxed">{children}</div>
    </div>
  );
}

export function AIAssistantPanel({
  patient,
  open,
  onClose,
}: {
  patient: Patient;
  open: boolean;
  onClose: () => void;
}) {
  return (
    <AnimatePresence>
      {open && (
        <motion.aside
          initial={{ x: 40, opacity: 0 }}
          animate={{ x: 0, opacity: 1 }}
          exit={{ x: 40, opacity: 0 }}
          transition={{ type: "spring", stiffness: 340, damping: 34 }}
          className="glass-panel sticky top-16 hidden h-[calc(100vh-4rem)] w-[340px] shrink-0 overflow-y-auto rounded-none border-y-0 border-r-0 p-4 scroll-slim xl:block"
        >
          <div className="mb-4 flex items-center justify-between">
            <p className="flex items-center gap-2 text-sm font-bold">
              <Sparkles className="size-4 text-primary" /> AI Clinical Copilot
            </p>
            <Button variant="ghost" size="icon" className="size-8 rounded-lg" onClick={onClose}>
              <X className="size-4" />
            </Button>
          </div>

          <div className="space-y-3">
            <div className="surface-card rounded-xl p-3">
              <div className="flex items-center justify-between">
                <p className="text-[10px] font-bold uppercase tracking-wide text-muted-foreground">
                  Patient risk score
                </p>
                <span className="num text-lg font-bold text-primary">{patient.riskScore}</span>
              </div>
              <Progress value={patient.riskScore} className="mt-2 h-1.5" />
              <p className="mt-2 text-[11px] text-muted-foreground">
                Composite of chronic load, vitals variance and adherence signals.
              </p>
            </div>

            <Section icon={Brain} title="Patient summary">
              <p className="text-muted-foreground">
                {patient.age}y {patient.gender.toLowerCase()} with {patient.chronic.join(", ")}. Last
                visit {patient.timeline[0]?.date}. {patient.timeline[0]?.aiSummary}
              </p>
            </Section>

            <Section icon={Activity} title="Previous diagnoses">
              {patient.timeline.map((v) => (
                <p key={v.id} className="text-muted-foreground">
                  <span className="num font-semibold text-foreground">{v.date}</span> — {v.diagnosis}
                </p>
              ))}
            </Section>

            <Section icon={Pill} title="Medicine history">
              <div className="flex flex-wrap gap-1">
                {patient.currentMedications.map((m) => (
                  <Badge key={m} variant="secondary" className="rounded-full text-[10px]">
                    {m}
                  </Badge>
                ))}
              </div>
            </Section>

            {patient.allergies.length > 0 && (
              <Section icon={AlertTriangle} title="Drug allergy alerts" tone="danger">
                <p>
                  Documented allergies: <b>{patient.allergies.join(", ")}</b>. Avoid cross-reactive
                  classes and verify before dispensing.
                </p>
              </Section>
            )}

            <Section icon={AlertTriangle} title="Drug interaction alerts" tone="danger">
              <p>
                Moderate: concurrent antihypertensive + NSAID may blunt BP control. Review before
                adding analgesics.
              </p>
            </Section>

            <Section icon={HeartPulse} title="Chronic disease summary">
              <p className="text-muted-foreground">
                {patient.chronic.length} active chronic conditions under management; medication load{" "}
                {patient.currentMedications.length}.
              </p>
            </Section>

            <Section icon={TrendingUp} title="Health trend">
              <p className="text-muted-foreground">
                BMI {patient.bmi} · SpO₂ {patient.vitals.spo2}% · BP {patient.vitals.bloodPressure}.
                Trend flagged {patient.riskScore > 60 ? "deteriorating" : "stable"} over 6 months.
              </p>
            </Section>

            <Section icon={ChevronRight} title="Suggested follow-up">
              <p className="text-muted-foreground">
                Review in {patient.riskScore > 60 ? "4 weeks" : "3 months"} with repeat labs. Enable
                voice adherence calls.
              </p>
            </Section>

            <p className="rounded-xl bg-muted/70 px-3 py-2.5 text-[11px] leading-relaxed text-muted-foreground">
              AI assists clinical decisions only. It never prescribes medicines automatically — every
              order requires doctor confirmation.
            </p>
          </div>
        </motion.aside>
      )}
    </AnimatePresence>
  );
}
