import { createFileRoute, Link } from "@tanstack/react-router";
import { Clock, Stethoscope, Loader2, Calendar, AlertCircle, RefreshCw } from "lucide-react";

import { AppShell, PageHeader } from "@/components/layout/app-shell";
import { useTodayAppointments, type AppointmentResponse, type AppointmentStatus } from "@/services/appointment-api";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/today")({
  head: () => ({
    meta: [
      { title: "Today's Patients — Meridian Health OS" },
      {
        name: "description",
        content: "Every patient scheduled with you today, with appointment status, visit type and instant consultation launch.",
      },
      { property: "og:title", content: "Today's Patients — Meridian Health OS" },
      { property: "og:description", content: "Full day roster with live appointment status." },
    ],
  }),
  component: Today,
});

const statusBadgeStyles: Record<AppointmentStatus, string> = {
  BOOKED: "bg-info/12 text-info border-info/20",
  CONFIRMED: "bg-amber-500/15 text-amber-700 dark:text-amber-400 border-amber-500/30",
  CHECKED_IN: "bg-primary/15 text-primary border-primary/30 font-semibold",
  IN_PROGRESS: "bg-emerald-500/15 text-emerald-700 dark:text-emerald-400 border-emerald-500/30 font-semibold",
  COMPLETED: "bg-success/15 text-success border-success/30",
  CANCELLED: "bg-destructive/15 text-destructive border-destructive/30",
  NO_SHOW: "bg-muted text-muted-foreground border-border",
};

function Today() {
  const { data, isLoading, isError, error, refetch, isRefetching } = useTodayAppointments();

  const appointments: AppointmentResponse[] = data?.content || [];

  return (
    <AppShell>
      <div className="space-y-6 p-4 sm:p-6">
        <PageHeader
          title="Today's Patients"
          subtitle={
            isLoading
              ? "Loading roster..."
              : `${appointments.length} patients on your appointment roster today`
          }
          actions={
            <Button
              variant="outline"
              size="sm"
              className="rounded-xl"
              onClick={() => refetch()}
              disabled={isRefetching}
            >
              <RefreshCw className={cn("size-3.5", isRefetching && "animate-spin")} />
              Refresh
            </Button>
          }
        />

        {/* Loading State */}
        {isLoading && (
          <div className="surface-card rounded-2xl p-6 flex flex-col items-center justify-center min-h-[200px]">
            <Loader2 className="size-8 animate-spin text-primary mb-2" />
            <p className="text-sm text-muted-foreground">Fetching today's appointments from server...</p>
          </div>
        )}

        {/* Error State */}
        {isError && (
          <div className="surface-card flex flex-col items-center justify-center rounded-2xl p-8 text-center">
            <AlertCircle className="size-10 text-destructive mb-2" />
            <h3 className="text-base font-bold">Unable to load today's roster</h3>
            <p className="mt-1 text-xs text-muted-foreground">
              {(error as any)?.message || "Failed to communicate with healthcare backend."}
            </p>
            <Button size="sm" className="mt-4 rounded-xl" onClick={() => refetch()}>
              Try Again
            </Button>
          </div>
        )}

        {/* Empty State */}
        {!isLoading && !isError && appointments.length === 0 && (
          <div className="surface-card flex flex-col items-center justify-center rounded-2xl p-12 text-center">
            <Calendar className="size-12 text-muted-foreground/50 mb-3" />
            <h3 className="text-base font-bold">No appointments scheduled for today</h3>
            <p className="mt-1 text-xs text-muted-foreground">
              Your appointment roster for today is clear.
            </p>
          </div>
        )}

        {/* Live Roster */}
        {!isLoading && !isError && appointments.length > 0 && (
          <div className="surface-card overflow-hidden rounded-2xl">
            <ul className="divide-y">
              {appointments.map((apt) => {
                const formattedTime = `${apt.startTime?.substring(0, 5) || "09:00"} - ${apt.endTime?.substring(0, 5) || "09:15"}`;

                return (
                  <li
                    key={apt.id}
                    className="flex flex-wrap items-center gap-3 px-4 py-3.5 hover:bg-primary-soft/50 transition-colors"
                  >
                    <div className="grid size-10 shrink-0 place-items-center rounded-lg border bg-primary/10 font-bold text-primary text-xs">
                      #{apt.id}
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-semibold">{apt.patientName}</p>
                      <p className="truncate text-xs text-muted-foreground">
                        Patient ID: {apt.patientId} · {apt.appointmentNumber} · {apt.appointmentType.replace("_", " ")}
                      </p>
                    </div>
                    <span className="num inline-flex shrink-0 items-center gap-1.5 text-xs text-muted-foreground">
                      <Clock className="size-3.5" /> {formattedTime}
                    </span>
                    <Badge className={cn("shrink-0 rounded-full border text-[10px]", statusBadgeStyles[apt.status])}>
                      {apt.status.replace("_", " ")}
                    </Badge>
                    <Link
                      to="/consultation/$patientId"
                      params={{ patientId: String(apt.patientId) }}
                      search={{ appointmentId: apt.id }}
                      className="shrink-0"
                    >
                      <Button size="sm" variant="outline" className="rounded-xl">
                        <Stethoscope className="size-3.5" /> Consult
                      </Button>
                    </Link>
                  </li>
                );
              })}
            </ul>
          </div>
        )}
      </div>
    </AppShell>
  );
}

