import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { motion } from "motion/react";
import {
  AlertTriangle,
  CalendarCheck,
  CheckCircle2,
  Clock,
  Hourglass,
  Repeat,
  Timer,
  Users,
  ChevronRight,
  Loader2,
} from "lucide-react";

import { AppShell, PageHeader } from "@/components/layout/app-shell";
import { StatCard } from "@/components/dashboard/stat-card";
import { QueuePanel } from "@/components/dashboard/queue-panel";
import { PatientSearch } from "@/components/dashboard/patient-search";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { getAuthUser, isAuthenticated } from "@/lib/auth";
import { queueApi, type QueueEntryResponse } from "@/services/queue-api";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "Doctor Dashboard — Meridian Health OS" },
      {
        name: "description",
        content:
          "AI-powered doctor dashboard: live patient queue, today's appointments, vitals, prescriptions and clinical insights in one workspace.",
      },
    ],
  }),
  component: Dashboard,
});

const timeline = [
  { time: "09:00", label: "Morning OPD begins", state: "done" },
  { time: "10:15", label: "Patient Check-ins active", state: "done" },
  { time: "10:30", label: "Consultation Queue active", state: "active" },
  { time: "13:00", label: "Lunch break", state: "next" },
] as const;

function Dashboard() {
  const navigate = useNavigate();
  const user = getAuthUser();

  useEffect(() => {
    if (!isAuthenticated()) {
      navigate({ to: "/login" });
    }
  }, [navigate]);

  // Query queue list for stats
  const { data: queueData } = useQuery({
    queryKey: ["doctor-queue"],
    queryFn: () => queueApi.getDoctorOwnQueue(),
    refetchInterval: 15000,
  });

  // Query current consultation token
  const { data: currentTokenData, isLoading: currentTokenLoading } = useQuery({
    queryKey: ["doctor-current-token"],
    queryFn: () => queueApi.getDoctorOwnCurrentConsultation(),
    refetchInterval: 10000,
  });

  const entries: QueueEntryResponse[] = queueData?.content || [];
  const waitingEntries = entries.filter((e) => e.status === "WAITING");
  const completedEntries = entries.filter((e) => e.status === "COMPLETED");
  const emergencyEntries = entries.filter((e) => e.priority === "EMERGENCY");
  const totalAppointments = queueData?.totalElements || entries.length;

  const nextPatient = waitingEntries[0];
  const doctorName = user?.email ? user.email.split("@")[0]?.replace(".", " ") ?? "Doctor" : "Doctor";

  return (
    <AppShell>
      <div className="space-y-6 p-4 sm:p-6">
        <PageHeader
          title={`Good morning, ${doctorName}`}
          subtitle={`Today · Live OPD Queue · Hospital #${user?.hospitalId || 1}`}
          actions={
            <Link to="/queue">
              <Button className="rounded-xl">
                Open live queue <ChevronRight className="size-4" />
              </Button>
            </Link>
          }
        />

        {/* Stat Cards connected to real API metrics */}
        <div className="grid grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-3 2xl:grid-cols-6">
          <StatCard
            index={0}
            label="Appointments"
            value={totalAppointments}
            icon={CalendarCheck}
            hint="today"
          />
          <StatCard
            index={1}
            label="Waiting"
            value={waitingEntries.length}
            icon={Hourglass}
            tone="warning"
            hint="live queue"
          />
          <StatCard
            index={2}
            label="Completed"
            value={completedEntries.length}
            icon={CheckCircle2}
            tone="success"
          />
          <StatCard
            index={3}
            label="Emergency"
            value={emergencyEntries.length}
            icon={AlertTriangle}
            tone="destructive"
            hint={emergencyEntries.length > 0 ? "active" : "none"}
          />
          <StatCard
            index={4}
            label="Skipped"
            value={entries.filter((e) => e.status === "SKIPPED").length}
            icon={Repeat}
            tone="info"
          />
          <StatCard
            index={5}
            label="In Consult"
            value={entries.filter((e) => e.status === "IN_CONSULTATION").length}
            icon={Users}
            tone="violet"
          />
        </div>

        <div className="grid gap-5 xl:grid-cols-[minmax(0,1fr)_360px]">
          <div className="space-y-5">
            <QueuePanel compact />
            <PatientSearch />
          </div>

          <div className="space-y-5">
            <div className="surface-card rounded-2xl p-4 sm:p-5">
              <h2 className="text-sm font-bold">Consultation Pulse</h2>
              <div className="mt-4 space-y-3">
                {/* Current Token Card */}
                <div className="rounded-xl bg-primary p-4 text-primary-foreground">
                  <p className="text-[10px] font-semibold uppercase tracking-wide opacity-80">
                    Current token
                  </p>
                  {currentTokenLoading ? (
                    <div className="flex items-center gap-2 py-2">
                      <Loader2 className="size-5 animate-spin" />
                      <span className="text-sm">Fetching token...</span>
                    </div>
                  ) : currentTokenData?.tokenNumber ? (
                    <div>
                      <p className="num text-3xl font-bold">#{currentTokenData.tokenNumber}</p>
                      <p className="mt-1 text-xs opacity-90">
                        {currentTokenData.patientName || `Patient ${currentTokenData.patientCode}`} · {currentTokenData.status.toLowerCase().replace("_", " ")}
                      </p>
                    </div>
                  ) : (
                    <div>
                      <p className="num text-2xl font-bold">None Active</p>
                      <p className="mt-1 text-xs opacity-90">Call next patient to start</p>
                    </div>
                  )}
                </div>

                {/* Next Patient Card */}
                {nextPatient && (
                  <div className="rounded-xl border bg-card/70 p-3">
                    <p className="text-[10px] font-semibold uppercase tracking-wide text-muted-foreground">
                      Next waiting patient
                    </p>
                    <div className="mt-1.5 flex items-center gap-2.5">
                      <div className="grid size-9 shrink-0 place-items-center rounded-lg border bg-primary/10 font-bold text-primary text-xs">
                        #{nextPatient.tokenNumber}
                      </div>
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-sm font-semibold">
                          {nextPatient.patientName || `Patient #${nextPatient.patientId}`}
                        </p>
                        <p className="num truncate text-[11px] text-muted-foreground">
                          {nextPatient.patientCode} · {nextPatient.priority}
                        </p>
                      </div>
                      <Link
                        to="/consultation/$patientId"
                        params={{ patientId: String(nextPatient.patientId) }}
                        className="ml-auto shrink-0"
                      >
                        <Button size="sm" className="rounded-lg">
                          Start
                        </Button>
                      </Link>
                    </div>
                  </div>
                )}

                <div className="grid grid-cols-2 gap-3">
                  <div className="rounded-xl border bg-card/70 p-3">
                    <p className="flex items-center gap-1.5 text-[10px] font-semibold uppercase tracking-wide text-muted-foreground">
                      <Timer className="size-3" /> Avg wait
                    </p>
                    <p className="num mt-1 text-xl font-bold">
                      {waitingEntries[0]?.estimatedWaitMinutes ?? 10} min
                    </p>
                  </div>
                  <div className="rounded-xl border bg-card/70 p-3">
                    <p className="flex items-center gap-1.5 text-[10px] font-semibold uppercase tracking-wide text-muted-foreground">
                      <Users className="size-3" /> Queue load
                    </p>
                    <p className="num mt-1 text-xl font-bold">
                      {totalAppointments > 0 ? Math.round((waitingEntries.length / totalAppointments) * 100) : 0}%
                    </p>
                  </div>
                </div>
              </div>
            </div>

            <div className="surface-card rounded-2xl p-4 sm:p-5">
              <h2 className="text-sm font-bold">Today's Timeline</h2>
              <ol className="mt-4 space-y-1 border-l pl-4">
                {timeline.map((t, i) => (
                  <motion.li
                    key={t.time}
                    initial={{ opacity: 0, x: -8 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: i * 0.05 }}
                    className="relative flex items-center gap-3 rounded-lg py-2 pl-2 pr-1 transition-colors hover:bg-muted/60"
                  >
                    <span
                      className={
                        "absolute -left-[21px] size-2.5 rounded-full border-2 border-background " +
                        (t.state === "active"
                          ? "bg-primary"
                          : t.state === "done"
                            ? "bg-success"
                            : "bg-muted-foreground/35")
                      }
                    />
                    <span className="num w-12 shrink-0 text-xs font-semibold text-muted-foreground">
                      {t.time}
                    </span>
                    <span className="min-w-0 flex-1 truncate text-sm">{t.label}</span>
                    {t.state === "active" && (
                      <Badge className="shrink-0 rounded-full border-0 bg-primary/12 text-[10px] text-primary">
                        <Clock className="size-3" /> now
                      </Badge>
                    )}
                  </motion.li>
                ))}
              </ol>
            </div>
          </div>
        </div>
      </div>
    </AppShell>
  );
}
