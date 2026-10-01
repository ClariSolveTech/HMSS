import { Link } from "@tanstack/react-router";
import { motion, AnimatePresence } from "motion/react";
import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  AlertTriangle,
  ChevronRight,
  Clock,
  PhoneCall,
  Play,
  SkipForward,
  RotateCcw,
  CheckCircle2,
  RefreshCw,
  Loader2,
} from "lucide-react";
import { toast } from "sonner";

import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { queueApi, type QueueEntryResponse, type BackendQueuePriority, type BackendQueueStatus } from "@/services/queue-api";

export const priorityStyles: Record<string, string> = {
  normal: "bg-secondary text-secondary-foreground",
  routine: "bg-secondary text-secondary-foreground",
  urgent: "bg-warning/15 text-warning",
  emergency: "bg-destructive/12 text-destructive",
};

const stateStyles: Record<string, string> = {
  waiting: "bg-info/12 text-info",
  called: "bg-amber-500/15 text-amber-600 font-semibold",
  "in-consultation": "bg-primary/12 text-primary font-semibold",
  completed: "bg-success/12 text-success",
  skipped: "bg-muted text-muted-foreground",
  cancelled: "bg-destructive/12 text-destructive",
};

const face = (id: number) =>
  `https://api.dicebear.com/9.x/notionists/svg?seed=Patient${id}&backgroundColor=dbeafe,e0f2fe,ede9fe`;

export function QueuePanel({ compact = false }: { compact?: boolean }) {
  const queryClient = useQueryClient();
  const [actionLoadingId, setActionLoadingId] = useState<number | null>(null);

  // Fetch real queue from backend
  const { data, isLoading, isError, error, refetch, isRefetching } = useQuery({
    queryKey: ["doctor-queue"],
    queryFn: () => queueApi.getDoctorOwnQueue(),
    refetchInterval: 15000, // Poll every 15 seconds
  });

  const invalidateQueue = () => {
    queryClient.invalidateQueries({ queryKey: ["doctor-queue"] });
    queryClient.invalidateQueries({ queryKey: ["doctor-current-token"] });
  };

  // Mutations for queue actions
  const callNextMutation = useMutation({
    mutationFn: () => queueApi.callNextPatientForDoctor(),
    onSuccess: (res) => {
      toast.success(`Called token #${res.tokenNumber} (${res.patientName || "Patient"})`);
      invalidateQueue();
    },
    onError: (err: any) => {
      toast.error(err.message || "Failed to call next patient");
    },
  });

  const callPatientMutation = useMutation({
    mutationFn: (id: number) => queueApi.callPatient(id),
    onSuccess: (res) => {
      toast.success(`Called token #${res.tokenNumber}`);
      invalidateQueue();
    },
    onError: (err: any) => toast.error(err.message || "Failed to call patient"),
    onSettled: () => setActionLoadingId(null),
  });

  const startConsultationMutation = useMutation({
    mutationFn: (id: number) => queueApi.startConsultation(id),
    onSuccess: (res) => {
      toast.success(`Started consultation for token #${res.tokenNumber}`);
      invalidateQueue();
    },
    onError: (err: any) => toast.error(err.message || "Failed to start consultation"),
    onSettled: () => setActionLoadingId(null),
  });

  const completeConsultationMutation = useMutation({
    mutationFn: (id: number) => queueApi.completeConsultation(id),
    onSuccess: (res) => {
      toast.success(`Completed consultation for token #${res.tokenNumber}`);
      invalidateQueue();
    },
    onError: (err: any) => toast.error(err.message || "Failed to complete consultation"),
    onSettled: () => setActionLoadingId(null),
  });

  const skipPatientMutation = useMutation({
    mutationFn: (id: number) => queueApi.skipPatient(id),
    onSuccess: (res) => {
      toast.success(`Skipped token #${res.tokenNumber}`);
      invalidateQueue();
    },
    onError: (err: any) => toast.error(err.message || "Failed to skip patient"),
    onSettled: () => setActionLoadingId(null),
  });

  const recallPatientMutation = useMutation({
    mutationFn: (id: number) => queueApi.recallPatient(id),
    onSuccess: (res) => {
      toast.success(`Recalled token #${res.tokenNumber}`);
      invalidateQueue();
    },
    onError: (err: any) => toast.error(err.message || "Failed to recall patient"),
    onSettled: () => setActionLoadingId(null),
  });

  const entries: QueueEntryResponse[] = data?.content || [];
  const waitingCount = entries.filter((r) => r.status === "WAITING").length;
  const visible = compact ? entries.filter((r) => r.status !== "COMPLETED" && r.status !== "CANCELLED").slice(0, 5) : entries;

  return (
    <div className="surface-card overflow-hidden rounded-2xl">
      <div className="grid grid-cols-[minmax(0,1fr)_auto] items-center gap-3 border-b px-4 py-3.5 sm:flex sm:justify-between sm:px-5">
        <div className="min-w-0">
          <div className="flex items-center gap-2">
            <h2 className="truncate text-sm font-bold">Live Queue</h2>
            {isRefetching && <Loader2 className="size-3.5 animate-spin text-muted-foreground" />}
          </div>
          <p className="text-xs text-muted-foreground">
            {waitingCount} waiting · synced with backend
          </p>
        </div>
        <div className="flex shrink-0 gap-2">
          <Button
            size="sm"
            variant="ghost"
            className="rounded-xl"
            onClick={() => refetch()}
            disabled={isRefetching}
            title="Refresh queue"
          >
            <RefreshCw className={cn("size-3.5", isRefetching && "animate-spin")} />
          </Button>
          <Button
            size="sm"
            variant="outline"
            className="rounded-xl"
            onClick={() => callNextMutation.mutate()}
            disabled={callNextMutation.isPending || waitingCount === 0}
          >
            {callNextMutation.isPending ? (
              <Loader2 className="size-3.5 animate-spin" />
            ) : (
              <PhoneCall className="size-3.5" />
            )}
            <span className="hidden sm:inline">Call Next</span>
          </Button>
        </div>
      </div>

      {isLoading ? (
        <div className="flex items-center justify-center p-12 text-sm text-muted-foreground">
          <Loader2 className="mr-2 size-5 animate-spin text-primary" /> Loading live queue...
        </div>
      ) : isError ? (
        <div className="p-6 text-center text-sm text-destructive">
          <p className="font-semibold">Failed to load queue</p>
          <p className="mt-1 text-xs text-muted-foreground">
            {(error as any)?.message || "Check network or backend connection."}
          </p>
          <Button size="sm" variant="outline" className="mt-3 rounded-xl" onClick={() => refetch()}>
            Try Again
          </Button>
        </div>
      ) : entries.length === 0 ? (
        <div className="p-8 text-center text-sm text-muted-foreground">
          <p className="font-semibold">No patients in queue today</p>
          <p className="mt-1 text-xs">Patients checked-in at reception will appear here live.</p>
        </div>
      ) : (
        <ul className="divide-y">
          <AnimatePresence initial={false}>
            {visible.map((row, i) => {
              const priorityKey = (row.priority || "NORMAL").toLowerCase();
              const displayStatus = (row.status || "WAITING").toLowerCase().replace("_", "-");
              const isActioning = actionLoadingId === row.id;

              return (
                <motion.li
                  key={row.id}
                  layout
                  initial={{ opacity: 0, y: 8 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0 }}
                  transition={{ delay: i * 0.03 }}
                  className="group flex flex-col gap-3 px-4 py-3.5 transition-colors hover:bg-primary-soft/60 sm:flex-row sm:items-center sm:px-5"
                >
                  <span className="num grid size-11 shrink-0 place-items-center rounded-xl bg-primary/10 text-[13px] font-bold text-primary">
                    #{row.tokenNumber}
                  </span>

                  <img
                    src={face(row.patientId || row.id)}
                    alt=""
                    className="size-10 shrink-0 rounded-lg border object-cover"
                  />

                  <div className="min-w-0 flex-1">
                    <div className="flex min-w-0 flex-wrap items-center gap-2">
                      <p className="truncate text-sm font-semibold">
                        {row.patientName || `Patient #${row.patientId}`}
                      </p>
                      {row.priority && row.priority !== "NORMAL" && (
                        <Badge className={cn("rounded-full border-0 text-[10px]", priorityStyles[priorityKey])}>
                          {row.priority === "EMERGENCY" && <AlertTriangle className="size-3 mr-1" />}
                          {row.priority}
                        </Badge>
                      )}
                    </div>
                    <p className="truncate text-xs text-muted-foreground">
                      {row.patientCode || `PID-${row.patientId}`}
                      {row.notes ? ` · ${row.notes}` : ""}
                    </p>
                  </div>

                  <div className="flex shrink-0 items-center gap-3">
                    <span className="inline-flex items-center gap-1.5 text-xs text-muted-foreground">
                      <Clock className="size-3.5" />
                      {row.status === "WAITING"
                        ? `${row.estimatedWaitMinutes ?? 0} min wait`
                        : row.status === "IN_CONSULTATION"
                          ? "In progress"
                          : row.status}
                    </span>
                    <Badge className={cn("rounded-full border-0 text-[10px] capitalize", stateStyles[displayStatus] || "bg-muted text-muted-foreground")}>
                      {displayStatus.replace("-", " ")}
                    </Badge>
                  </div>

                  <div className="flex shrink-0 gap-2 items-center">
                    {/* Action buttons based on Queue Status */}
                    {row.status === "WAITING" && (
                      <>
                        <Button
                          size="sm"
                          variant="ghost"
                          className="rounded-xl"
                          disabled={isActioning}
                          onClick={() => {
                            setActionLoadingId(row.id);
                            skipPatientMutation.mutate(row.id);
                          }}
                          title="Skip Patient"
                        >
                          <SkipForward className="size-3.5" />
                          <span className="sr-only">Skip</span>
                        </Button>
                        <Button
                          size="sm"
                          variant="outline"
                          className="rounded-xl"
                          disabled={isActioning}
                          onClick={() => {
                            setActionLoadingId(row.id);
                            callPatientMutation.mutate(row.id);
                          }}
                        >
                          {isActioning ? (
                            <Loader2 className="size-3.5 animate-spin" />
                          ) : (
                            <PhoneCall className="size-3.5" />
                          )}
                          <span className="hidden sm:inline ml-1">Call</span>
                        </Button>
                        <Button
                          size="sm"
                          className="rounded-xl"
                          disabled={isActioning}
                          onClick={() => {
                            setActionLoadingId(row.id);
                            startConsultationMutation.mutate(row.id);
                          }}
                        >
                          {isActioning ? (
                            <Loader2 className="size-3.5 animate-spin" />
                          ) : (
                            <Play className="size-3.5" />
                          )}
                          <span className="hidden sm:inline ml-1">Start</span>
                        </Button>
                      </>
                    )}

                    {row.status === "CALLED" && (
                      <>
                        <Button
                          size="sm"
                          variant="ghost"
                          className="rounded-xl"
                          disabled={isActioning}
                          onClick={() => {
                            setActionLoadingId(row.id);
                            skipPatientMutation.mutate(row.id);
                          }}
                        >
                          <SkipForward className="size-3.5" />
                          <span className="sr-only">Skip</span>
                        </Button>
                        <Button
                          size="sm"
                          className="rounded-xl"
                          disabled={isActioning}
                          onClick={() => {
                            setActionLoadingId(row.id);
                            startConsultationMutation.mutate(row.id);
                          }}
                        >
                          {isActioning ? (
                            <Loader2 className="size-3.5 animate-spin" />
                          ) : (
                            <Play className="size-3.5" />
                          )}
                          <span className="hidden sm:inline ml-1">Start</span>
                        </Button>
                      </>
                    )}

                    {row.status === "IN_CONSULTATION" && (
                      <Button
                        size="sm"
                        variant="default"
                        className="rounded-xl bg-success hover:bg-success/90 text-success-foreground"
                        disabled={isActioning}
                        onClick={() => {
                          setActionLoadingId(row.id);
                          completeConsultationMutation.mutate(row.id);
                        }}
                      >
                        {isActioning ? (
                          <Loader2 className="size-3.5 animate-spin" />
                        ) : (
                          <CheckCircle2 className="size-3.5" />
                        )}
                        <span className="hidden sm:inline ml-1">Complete</span>
                      </Button>
                    )}

                    {row.status === "SKIPPED" && (
                      <Button
                        size="sm"
                        variant="outline"
                        className="rounded-xl"
                        disabled={isActioning}
                        onClick={() => {
                          setActionLoadingId(row.id);
                          recallPatientMutation.mutate(row.id);
                        }}
                      >
                        {isActioning ? (
                          <Loader2 className="size-3.5 animate-spin" />
                        ) : (
                          <RotateCcw className="size-3.5" />
                        )}
                        <span className="hidden sm:inline ml-1">Recall</span>
                      </Button>
                    )}

                    <Link to="/consultation/$patientId" params={{ patientId: String(row.patientId) }}>
                      <Button size="sm" variant="ghost" className="rounded-xl" title="Open patient chart">
                        <ChevronRight className="size-3.5 opacity-70" />
                      </Button>
                    </Link>
                  </div>
                </motion.li>
              );
            })}
          </AnimatePresence>
        </ul>
      )}
    </div>
  );
}
