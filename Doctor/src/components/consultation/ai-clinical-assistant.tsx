import { useState } from "react";
import { AnimatePresence, motion } from "motion/react";
import {
  AlertTriangle,
  Bot,
  ChevronDown,
  ClipboardPen,
  FlaskConical,
  Loader2,
  RefreshCw,
  Sparkles,
  Stethoscope,
  User,
  X,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";
import {
  type ClinicalAssistantAction,
  type ClinicalAssistantResponse,
  useClinicalAssistant,
} from "@/services/ai-api";
import { ApiError } from "@/lib/api-client";

// ─── Action definitions ───────────────────────────────────────────────────────

interface ActionDef {
  id: ClinicalAssistantAction;
  label: string;
  icon: typeof Bot;
  requiresConsultation: boolean;
  description: string;
}

const ACTIONS: ActionDef[] = [
  {
    id: "SUMMARIZE_PATIENT",
    label: "Summarize Patient",
    icon: User,
    requiresConsultation: false,
    description: "Patient demographics, history, medications & conditions",
  },
  {
    id: "SUMMARIZE_CONSULTATION",
    label: "Summarize Consultation",
    icon: Stethoscope,
    requiresConsultation: true,
    description: "Chief complaint, clinical findings & current diagnosis",
  },
  {
    id: "ANALYZE_LAB_RESULTS",
    label: "Analyze Lab Results",
    icon: FlaskConical,
    requiresConsultation: false,
    description: "Interpret recent lab findings in clinical context",
  },
  {
    id: "SUGGEST_CLINICAL_CONSIDERATIONS",
    label: "Clinical Considerations",
    icon: Sparkles,
    requiresConsultation: true,
    description: "Evidence-based differential & management pointers",
  },
  {
    id: "DRAFT_CLINICAL_NOTE",
    label: "Draft Clinical Note",
    icon: ClipboardPen,
    requiresConsultation: true,
    description: "Generate a structured clinical note draft",
  },
  {
    id: "EXPLAIN_PATIENT_HISTORY",
    label: "Explain Patient History",
    icon: RefreshCw,
    requiresConsultation: false,
    description: "Summarise longitudinal history from previous consultations",
  },
];

// ─── Human-readable action label ──────────────────────────────────────────────

function actionLabel(action: ClinicalAssistantAction): string {
  return ACTIONS.find((a) => a.id === action)?.label ?? action;
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

function formatTimestamp(raw: string | undefined): string | null {
  if (!raw) return null;
  try {
    return new Date(raw).toLocaleTimeString([], {
      hour: "2-digit",
      minute: "2-digit",
    });
  } catch {
    return null;
  }
}

function extractErrorMessage(err: unknown): string {
  if (err instanceof ApiError) {
    // Return the backend's safe message; never expose internals
    return err.message || "The AI assistant returned an error. Please try again.";
  }
  if (err instanceof Error) {
    return err.message || "An unexpected error occurred.";
  }
  return "An unexpected error occurred.";
}

// ─── Response panel ───────────────────────────────────────────────────────────

function AIResponsePanel({
  response,
  isLoading,
  error,
  activeAction,
}: {
  response: ClinicalAssistantResponse | undefined;
  isLoading: boolean;
  error: unknown;
  activeAction: ClinicalAssistantAction | null;
}) {
  if (isLoading) {
    return (
      <div
        className="rounded-xl border bg-card/60 p-4 space-y-3"
        role="status"
        aria-label="Generating AI response…"
        aria-live="polite"
      >
        <div className="flex items-center gap-2">
          <Loader2 className="size-3.5 animate-spin text-primary" />
          <span className="text-[11px] font-semibold text-primary">
            Generating{activeAction ? ` · ${actionLabel(activeAction)}` : ""}…
          </span>
        </div>
        <Skeleton className="h-3 w-full rounded-md" />
        <Skeleton className="h-3 w-[90%] rounded-md" />
        <Skeleton className="h-3 w-[75%] rounded-md" />
        <Skeleton className="h-3 w-[85%] rounded-md" />
        <Skeleton className="h-3 w-[60%] rounded-md" />
      </div>
    );
  }

  if (error) {
    return (
      <div
        className="rounded-xl border border-destructive/30 bg-destructive/5 p-4 space-y-2"
        role="alert"
        aria-live="assertive"
      >
        <p className="flex items-center gap-1.5 text-[11px] font-bold uppercase tracking-wide text-destructive">
          <AlertTriangle className="size-3.5" /> AI Error
        </p>
        <p className="text-xs text-destructive/90 leading-relaxed">
          {extractErrorMessage(error)}
        </p>
      </div>
    );
  }

  if (!response) return null;

  const time = formatTimestamp(response.timestamp);

  return (
    <motion.div
      initial={{ opacity: 0, y: 6 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.25 }}
      className="rounded-xl border border-primary/20 bg-primary/5 p-4 space-y-3"
      role="region"
      aria-label="AI Clinical Response"
    >
      {/* Header */}
      <div className="flex items-center justify-between gap-2">
        <Badge
          variant="secondary"
          className="rounded-full text-[10px] font-semibold px-2 py-0.5 bg-primary/10 text-primary border-0"
        >
          AI — {actionLabel(response.action)}
        </Badge>
        {time && (
          <span className="text-[10px] text-muted-foreground tabular-nums">
            {time}
          </span>
        )}
      </div>

      {/* Source summary */}
      {response.sourceSummary && (
        <p className="text-[10px] font-semibold uppercase tracking-wide text-muted-foreground">
          {response.sourceSummary}
        </p>
      )}

      {/* Generated content */}
      <div className="text-xs leading-relaxed text-foreground whitespace-pre-wrap">
        {response.generatedContent}
      </div>

      {/* Disclaimer */}
      <div className="rounded-lg bg-warning/10 border border-warning/20 px-3 py-2">
        <p className="flex items-start gap-1.5 text-[10px] leading-relaxed text-warning-foreground">
          <AlertTriangle className="mt-0.5 size-3 shrink-0 text-warning" />
          <span>
            {response.disclaimer ||
              "AI-generated clinical decision support. Doctor review is required before any clinical action."}
          </span>
        </p>
      </div>
    </motion.div>
  );
}

// ─── Main component ───────────────────────────────────────────────────────────

export interface AIClinicalAssistantProps {
  /** Numeric patient ID from the backend — required */
  patientId: number;
  /** Numeric consultation ID — undefined if the consultation hasn't been saved yet */
  consultationId: number | undefined;
  open: boolean;
  onClose: () => void;
}

export function AIClinicalAssistant({
  patientId,
  consultationId,
  open,
  onClose,
}: AIClinicalAssistantProps) {
  const [activeAction, setActiveAction] =
    useState<ClinicalAssistantAction | null>(null);
  const [showAllActions, setShowAllActions] = useState(false);

  const {
    mutate: requestAI,
    data: response,
    isPending: isLoading,
    error,
    reset: resetResult,
  } = useClinicalAssistant();

  function handleAction(action: ClinicalAssistantAction) {
    if (isLoading) return; // prevent duplicate submissions
    setActiveAction(action);
    resetResult();
    requestAI({ patientId, consultationId, action });
  }

  // Show first 3 by default; expand to all 6 on demand
  const visibleActions = showAllActions ? ACTIONS : ACTIONS.slice(0, 3);

  return (
    <AnimatePresence>
      {open && (
        <motion.aside
          initial={{ x: 40, opacity: 0 }}
          animate={{ x: 0, opacity: 1 }}
          exit={{ x: 40, opacity: 0 }}
          transition={{ type: "spring", stiffness: 340, damping: 34 }}
          className="glass-panel sticky top-16 hidden h-[calc(100vh-4rem)] w-[340px] shrink-0 flex-col overflow-y-auto rounded-none border-y-0 border-r-0 p-4 scroll-slim xl:flex"
          aria-label="AI Clinical Assistant panel"
        >
          {/* ── Header ── */}
          <div className="mb-4 flex items-start justify-between gap-2">
            <div>
              <p className="flex items-center gap-2 text-sm font-bold">
                <Bot className="size-4 text-primary" />
                AI Clinical Assistant
              </p>
              <p className="mt-0.5 text-[11px] text-muted-foreground leading-snug">
                Clinical decision-support · Doctor review required
              </p>
            </div>
            <Button
              variant="ghost"
              size="icon"
              className="size-8 shrink-0 rounded-lg"
              onClick={onClose}
              aria-label="Close AI Clinical Assistant"
            >
              <X className="size-4" />
            </Button>
          </div>

          {/* ── Context badge ── */}
          <div className="mb-3 flex flex-wrap items-center gap-1.5">
            <Badge
              variant="outline"
              className={cn(
                "rounded-full text-[10px] px-2 py-0.5",
                consultationId
                  ? "border-success/40 text-success"
                  : "border-muted-foreground/30 text-muted-foreground"
              )}
            >
              {consultationId
                ? `Consultation #${consultationId}`
                : "No active consultation"}
            </Badge>
            <Badge
              variant="outline"
              className="rounded-full text-[10px] px-2 py-0.5 border-primary/30 text-primary"
            >
              Patient #{patientId}
            </Badge>
          </div>

          {/* ── Action buttons ── */}
          <div className="space-y-1.5">
            {visibleActions.map((action) => {
              const Icon = action.icon;
              const disabled =
                isLoading ||
                (action.requiresConsultation && !consultationId);
              const isActive = activeAction === action.id && isLoading;

              return (
                <button
                  key={action.id}
                  disabled={disabled}
                  onClick={() => handleAction(action.id)}
                  aria-label={action.label}
                  aria-disabled={disabled}
                  className={cn(
                    "w-full rounded-xl border px-3 py-2.5 text-left transition-all",
                    "flex items-start gap-3",
                    disabled
                      ? "cursor-not-allowed opacity-40 bg-muted/30"
                      : "hover:border-primary/40 hover:bg-primary/5 cursor-pointer",
                    isActive && "border-primary/40 bg-primary/5"
                  )}
                >
                  <span className="mt-0.5 grid size-6 shrink-0 place-items-center rounded-lg bg-primary/10">
                    {isActive ? (
                      <Loader2 className="size-3.5 animate-spin text-primary" />
                    ) : (
                      <Icon className="size-3.5 text-primary" />
                    )}
                  </span>
                  <span className="min-w-0">
                    <span className="block text-xs font-semibold leading-tight">
                      {action.label}
                    </span>
                    <span className="block text-[10px] text-muted-foreground leading-snug mt-0.5 truncate">
                      {action.requiresConsultation && !consultationId
                        ? "Requires saved consultation"
                        : action.description}
                    </span>
                  </span>
                </button>
              );
            })}
          </div>

          {/* ── Show more / fewer toggle ── */}
          <button
            onClick={() => setShowAllActions((v) => !v)}
            className="mt-2 flex items-center gap-1 text-[11px] font-medium text-muted-foreground hover:text-foreground transition-colors"
            aria-expanded={showAllActions}
            aria-label={showAllActions ? "Show fewer actions" : "Show more actions"}
          >
            <ChevronDown
              className={cn(
                "size-3.5 transition-transform",
                showAllActions && "rotate-180"
              )}
            />
            {showAllActions ? "Show fewer" : `Show all ${ACTIONS.length} actions`}
          </button>

          {/* ── Divider ── */}
          <div className="my-4 border-t" />

          {/* ── Response area ── */}
          <AIResponsePanel
            response={response}
            isLoading={isLoading}
            error={error}
            activeAction={activeAction}
          />

          {/* ── Standing safety notice (always visible) ── */}
          <p className="mt-auto pt-4 rounded-xl bg-muted/50 px-3 py-2.5 text-[11px] leading-relaxed text-muted-foreground">
            <span className="font-semibold">⚠ AI output is clinical decision support only.</span>{" "}
            Every clinical action — prescriptions, diagnoses, lab orders — requires the
            doctor's own independent judgement. AI responses do not automatically update
            any patient record.
          </p>
        </motion.aside>
      )}
    </AnimatePresence>
  );
}
