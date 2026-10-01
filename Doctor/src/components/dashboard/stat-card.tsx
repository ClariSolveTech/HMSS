import { motion } from "motion/react";
import type { LucideIcon } from "lucide-react";
import { TrendingDown, TrendingUp } from "lucide-react";

import { cn } from "@/lib/utils";

export type StatTone = "primary" | "warning" | "success" | "destructive" | "info" | "violet";

const toneMap: Record<StatTone, { icon: string; ring: string }> = {
  primary: { icon: "bg-primary/10 text-primary", ring: "group-hover:border-primary/40" },
  warning: { icon: "bg-warning/15 text-warning", ring: "group-hover:border-warning/40" },
  success: { icon: "bg-success/12 text-success", ring: "group-hover:border-success/40" },
  destructive: {
    icon: "bg-destructive/12 text-destructive",
    ring: "group-hover:border-destructive/40",
  },
  info: { icon: "bg-info/12 text-info", ring: "group-hover:border-info/40" },
  violet: { icon: "bg-violet/12 text-violet", ring: "group-hover:border-violet/40" },
};

export function StatCard({
  label,
  value,
  hint,
  delta,
  icon: Icon,
  tone = "primary",
  index = 0,
}: {
  label: string;
  value: string | number;
  hint?: string;
  delta?: number;
  icon: LucideIcon;
  tone?: StatTone;
  index?: number;
}) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 14 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.05, duration: 0.35, ease: "easeOut" }}
      whileHover={{ y: -3 }}
      className={cn(
        "group surface-card relative overflow-hidden rounded-2xl p-4 transition-colors sm:p-5",
        toneMap[tone].ring,
      )}
    >
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="truncate text-xs font-semibold uppercase tracking-wide text-muted-foreground">
            {label}
          </p>
          <p className="num mt-2 text-3xl font-bold">{value}</p>
        </div>
        <span className={cn("grid size-10 shrink-0 place-items-center rounded-xl", toneMap[tone].icon)}>
          <Icon className="size-5" />
        </span>
      </div>
      <div className="mt-3 flex items-center gap-2 text-xs">
        {typeof delta === "number" && (
          <span
            className={cn(
              "inline-flex items-center gap-1 rounded-full px-2 py-0.5 font-semibold",
              delta >= 0 ? "bg-success/12 text-success" : "bg-destructive/12 text-destructive",
            )}
          >
            {delta >= 0 ? <TrendingUp className="size-3" /> : <TrendingDown className="size-3" />}
            {Math.abs(delta)}%
          </span>
        )}
        {hint && <span className="truncate text-muted-foreground">{hint}</span>}
      </div>
    </motion.div>
  );
}
