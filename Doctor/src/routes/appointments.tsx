import { createFileRoute, Link } from "@tanstack/react-router";
import { useState } from "react";
import {
  Clock,
  MapPin,
  CheckCircle2,
  UserCheck,
  XCircle,
  Loader2,
  RefreshCw,
  AlertCircle,
  Calendar,
  Stethoscope,
  Search,
  UserX,
  X,
  Filter,
} from "lucide-react";

import { AppShell, PageHeader } from "@/components/layout/app-shell";
import {
  useAppointments,
  useConfirmAppointment,
  useCheckInAppointment,
  useCancelAppointment,
  useMarkNoShow,
  type AppointmentResponse,
  type AppointmentStatus,
  type AppointmentType,
} from "@/services/appointment-api";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/appointments")({
  head: () => ({
    meta: [
      { title: "Appointments — Meridian Health OS" },
      {
        name: "description",
        content:
          "Slot-wise appointment schedule with live search, filters, check-ins and instant consultation launch.",
      },
      { property: "og:title", content: "Appointments — Meridian Health OS" },
      { property: "og:description", content: "Search and manage patient appointments." },
    ],
  }),
  component: Appointments,
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

function Appointments() {
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedDate, setSelectedDate] = useState("");
  const [selectedStatus, setSelectedStatus] = useState<AppointmentStatus | "ALL">("ALL");
  const [selectedType, setSelectedType] = useState<AppointmentType | "ALL">("ALL");

  const { data, isLoading, isError, error, refetch, isRefetching } = useAppointments({
    appointmentDate: selectedDate || undefined,
    status: selectedStatus !== "ALL" ? selectedStatus : undefined,
    appointmentType: selectedType !== "ALL" ? selectedType : undefined,
    search: searchQuery.trim() || undefined,
  });

  const confirmMutation = useConfirmAppointment();
  const checkInMutation = useCheckInAppointment();
  const cancelMutation = useCancelAppointment();
  const noShowMutation = useMarkNoShow();
  const [actionId, setActionId] = useState<number | null>(null);

  const appointments: AppointmentResponse[] = data?.content || [];
  const hasActiveFilters = Boolean(searchQuery || selectedDate || selectedStatus !== "ALL" || selectedType !== "ALL");

  const handleClearFilters = () => {
    setSearchQuery("");
    setSelectedDate("");
    setSelectedStatus("ALL");
    setSelectedType("ALL");
  };

  const handleConfirm = (id: number) => {
    setActionId(id);
    confirmMutation.mutate(id, {
      onSettled: () => setActionId(null),
    });
  };

  const handleCheckIn = (id: number) => {
    setActionId(id);
    checkInMutation.mutate(id, {
      onSettled: () => setActionId(null),
    });
  };

  const handleNoShow = (id: number) => {
    if (!window.confirm("Mark this appointment as NO-SHOW?")) return;
    setActionId(id);
    noShowMutation.mutate(id, {
      onSettled: () => setActionId(null),
    });
  };

  const handleCancel = (id: number) => {
    if (!window.confirm("Are you sure you want to cancel this appointment?")) return;
    setActionId(id);
    cancelMutation.mutate(
      { id, reason: "Cancelled by doctor" },
      { onSettled: () => setActionId(null) }
    );
  };

  return (
    <AppShell>
      <div className="space-y-6 p-4 sm:p-6">
        <PageHeader
          title="Appointments"
          subtitle={
            isLoading
              ? "Loading appointments schedule..."
              : `${appointments.length} appointments found${selectedDate ? ` for ${selectedDate}` : ""}`
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

        {/* Filter Controls Bar */}
        <div className="surface-card rounded-2xl p-4 space-y-3">
          <div className="flex items-center gap-2 text-xs font-semibold text-muted-foreground uppercase tracking-wide">
            <Filter className="size-3.5 text-primary" /> Filter & Search Roster
          </div>

          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            {/* Search Input */}
            <div className="relative">
              <Search className="absolute left-3 top-2.5 size-4 text-muted-foreground" />
              <Input
                placeholder="Search patient name or apt #..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-9 rounded-xl text-xs"
              />
            </div>

            {/* Date Filter */}
            <div className="relative">
              <Input
                type="date"
                value={selectedDate}
                onChange={(e) => setSelectedDate(e.target.value)}
                className="rounded-xl text-xs"
              />
            </div>

            {/* Status Filter */}
            <div>
              <select
                value={selectedStatus}
                onChange={(e) => setSelectedStatus(e.target.value as AppointmentStatus | "ALL")}
                className="w-full h-9 rounded-xl border border-input bg-background px-3 py-1 text-xs shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
              >
                <option value="ALL">All Statuses</option>
                <option value="BOOKED">Booked</option>
                <option value="CONFIRMED">Confirmed</option>
                <option value="CHECKED_IN">Checked In</option>
                <option value="IN_PROGRESS">In Progress</option>
                <option value="COMPLETED">Completed</option>
                <option value="CANCELLED">Cancelled</option>
                <option value="NO_SHOW">No Show</option>
              </select>
            </div>

            {/* Appointment Type Filter */}
            <div>
              <select
                value={selectedType}
                onChange={(e) => setSelectedType(e.target.value as AppointmentType | "ALL")}
                className="w-full h-9 rounded-xl border border-input bg-background px-3 py-1 text-xs shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
              >
                <option value="ALL">All Visit Types</option>
                <option value="NEW_CONSULTATION">New Consultation</option>
                <option value="FOLLOW_UP">Follow Up</option>
                <option value="EMERGENCY">Emergency</option>
                <option value="WALK_IN">Walk In</option>
              </select>
            </div>
          </div>

          {/* Reset Filters */}
          {hasActiveFilters && (
            <div className="flex justify-end pt-1">
              <Button
                variant="ghost"
                size="sm"
                className="rounded-xl text-xs text-muted-foreground hover:text-foreground"
                onClick={handleClearFilters}
              >
                <X className="size-3.5" /> Clear Filters
              </Button>
            </div>
          )}
        </div>

        {/* Loading State */}
        {isLoading && (
          <div className="grid gap-3 md:grid-cols-2 2xl:grid-cols-3">
            {[1, 2, 3, 4, 5, 6].map((n) => (
              <div key={n} className="surface-card animate-pulse rounded-2xl p-4 space-y-3">
                <div className="h-5 w-1/2 rounded bg-muted/60" />
                <div className="h-4 w-1/3 rounded bg-muted/40" />
                <div className="h-9 w-full rounded-xl bg-muted/50" />
              </div>
            ))}
          </div>
        )}

        {/* Error State */}
        {isError && (
          <div className="surface-card flex flex-col items-center justify-center rounded-2xl p-8 text-center">
            <AlertCircle className="size-10 text-destructive mb-2" />
            <h3 className="text-base font-bold">Failed to load appointments</h3>
            <p className="mt-1 text-xs text-muted-foreground">
              {(error as any)?.message || "Network error while connecting to backend server."}
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
            <h3 className="text-base font-bold">No appointments found</h3>
            <p className="mt-1 text-xs text-muted-foreground">
              {hasActiveFilters
                ? "No appointments match your current search and filter criteria."
                : "No appointments scheduled for the selected view."}
            </p>
            {hasActiveFilters && (
              <Button size="sm" variant="outline" className="mt-4 rounded-xl" onClick={handleClearFilters}>
                Clear Filters
              </Button>
            )}
          </div>
        )}

        {/* Appointments List */}
        {!isLoading && !isError && appointments.length > 0 && (
          <div className="grid gap-3 md:grid-cols-2 2xl:grid-cols-3">
            {appointments.map((apt) => {
              const isWorking = actionId === apt.id;
              const formattedTime = `${apt.startTime?.substring(0, 5) || "09:00"} - ${apt.endTime?.substring(0, 5) || "09:15"}`;

              return (
                <div key={apt.id} className="surface-card rounded-2xl p-4 flex flex-col justify-between space-y-4">
                  <div>
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-sm font-semibold">{apt.patientName}</p>
                        <p className="num truncate text-xs text-muted-foreground">
                          #{apt.appointmentNumber} · Patient ID: {apt.patientId}
                        </p>
                      </div>
                      <Badge className={cn("shrink-0 rounded-full border text-[10px]", statusBadgeStyles[apt.status])}>
                        {apt.status.replace("_", " ")}
                      </Badge>
                    </div>

                    <div className="mt-3 space-y-1.5 text-xs text-muted-foreground">
                      <div className="flex items-center gap-2">
                        <Clock className="size-3.5 text-primary" />
                        <span className="font-medium text-foreground">{formattedTime}</span>
                        <span className="text-[11px]">({apt.appointmentDate})</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <MapPin className="size-3.5" />
                        <span>Type: {apt.appointmentType.replace("_", " ")}</span>
                      </div>
                      {apt.reason && (
                        <p className="mt-1 line-clamp-2 text-[11px] italic bg-muted/40 p-2 rounded-lg">
                          "{apt.reason}"
                        </p>
                      )}
                    </div>
                  </div>

                  {/* Actions Bar */}
                  <div className="flex flex-wrap items-center gap-2 pt-2 border-t">
                    {apt.status === "BOOKED" && (
                      <Button
                        size="sm"
                        variant="outline"
                        className="flex-1 rounded-xl text-xs"
                        onClick={() => handleConfirm(apt.id)}
                        disabled={isWorking}
                      >
                        {isWorking ? <Loader2 className="size-3.5 animate-spin" /> : <CheckCircle2 className="size-3.5 text-amber-600" />}
                        Confirm
                      </Button>
                    )}

                    {(apt.status === "BOOKED" || apt.status === "CONFIRMED") && (
                      <Button
                        size="sm"
                        variant="default"
                        className="flex-1 rounded-xl text-xs"
                        onClick={() => handleCheckIn(apt.id)}
                        disabled={isWorking}
                      >
                        {isWorking ? <Loader2 className="size-3.5 animate-spin" /> : <UserCheck className="size-3.5" />}
                        Check-in
                      </Button>
                    )}

                    {(apt.status === "BOOKED" || apt.status === "CONFIRMED" || apt.status === "CHECKED_IN") && (
                      <Button
                        size="sm"
                        variant="outline"
                        className="rounded-xl text-xs text-muted-foreground hover:bg-muted"
                        onClick={() => handleNoShow(apt.id)}
                        disabled={isWorking}
                      >
                        {isWorking ? <Loader2 className="size-3.5 animate-spin" /> : <UserX className="size-3.5" />}
                        No-Show
                      </Button>
                    )}

                    {apt.status !== "CANCELLED" && apt.status !== "COMPLETED" && (
                      <Button
                        size="sm"
                        variant="ghost"
                        className="rounded-xl text-xs text-destructive hover:bg-destructive/10"
                        onClick={() => handleCancel(apt.id)}
                        disabled={isWorking}
                      >
                        <XCircle className="size-3.5" />
                        Cancel
                      </Button>
                    )}

                    <Link
                      to="/consultation/$patientId"
                      params={{ patientId: String(apt.patientId) }}
                      search={{ appointmentId: apt.id }}
                      className="w-full mt-1"
                    >
                      <Button size="sm" variant="secondary" className="w-full rounded-xl text-xs">
                        <Stethoscope className="size-3.5" /> Launch Consultation
                      </Button>
                    </Link>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </AppShell>
  );
}


