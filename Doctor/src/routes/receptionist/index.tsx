import { createFileRoute, Link } from "@tanstack/react-router";
import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { motion } from "motion/react";
import {
  CalendarDays,
  CheckCircle2,
  Clock,
  Hourglass,
  Users,
  UserPlus,
  CalendarPlus,
  Footprints,
  ListOrdered,
  ArrowRight,
  RefreshCw,
  AlertCircle,
  Search,
  Filter,
  Stethoscope,
} from "lucide-react";

import { PageHeader } from "@/components/layout/app-shell";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { appointmentApi, type AppointmentResponse } from "@/services/appointment-api";
import { queueApi, type QueueEntryResponse } from "@/services/queue-api";
import { getAuthUser } from "@/lib/auth";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/receptionist/")({
  head: () => ({
    meta: [
      { title: "Receptionist Dashboard — Meridian Health OS" },
      { name: "description", content: "Front desk reception, live queue token dispatch, and patient check-in." },
    ],
  }),
  component: ReceptionistDashboard,
});

function getDeterministicLocalDate(d = new Date()) {
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function ReceptionistDashboard() {
  const authUser = getAuthUser();
  const todayDateStr = getDeterministicLocalDate();

  // Fetch today's appointments
  const {
    data: todayApptData,
    isLoading: loadingAppts,
    isError: errorAppts,
    refetch: refetchAppts,
  } = useQuery({
    queryKey: ["receptionist-today-appointments"],
    queryFn: () => appointmentApi.getTodayAppointments(undefined, 0, 100),
  });

  // Fetch today's queue
  const {
    data: todayQueueData,
    isLoading: loadingQueue,
    isError: errorQueue,
    refetch: refetchQueue,
  } = useQuery({
    queryKey: ["receptionist-today-queue"],
    queryFn: () => queueApi.getTodayQueue(undefined, 0, 100),
  });

  const appointments = todayApptData?.content || [];
  const queueEntries = todayQueueData?.content || [];

  const totalToday = appointments.length;
  const confirmedCount = appointments.filter((a) => a.status === "CONFIRMED").length;
  const waitingInQueue = queueEntries.filter((q) => q.status === "WAITING" || q.status === "CALLED").length;
  const completedCount =
    appointments.filter((a) => a.status === "COMPLETED").length +
    queueEntries.filter((q) => q.status === "COMPLETED").length;
  const cancelledCount = appointments.filter((a) => a.status === "CANCELLED").length;

  const [searchQuery, setSearchQuery] = useState("");
  const filteredAppointments = appointments.filter((a) => {
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase();
    return (
      a.patientName?.toLowerCase().includes(q) ||
      a.doctorName?.toLowerCase().includes(q) ||
      a.appointmentNumber?.toLowerCase().includes(q)
    );
  });

  return (
    <div className="space-y-6 p-4 sm:p-6 lg:p-8">
      {/* Page Header */}
      <PageHeader
        title="Reception Desk"
        subtitle={`Welcome back, ${authUser?.firstName || "Receptionist"}. City Central Hospital · Live Front Desk.`}
        actions={
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              className="rounded-xl gap-2"
              onClick={() => {
                refetchAppts();
                refetchQueue();
              }}
            >
              <RefreshCw className="size-3.5" /> Refresh
            </Button>
            <Link to="/receptionist/check-in">
              <Button size="sm" className="rounded-xl gap-2">
                <CheckCircle2 className="size-4" /> Quick Check-In
              </Button>
            </Link>
          </div>
        }
      />

      {/* Quick Action Strip */}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
        <Link
          to="/receptionist/patients"
          search={{ action: "register" }}
          className="surface-card group flex flex-col gap-2 rounded-2xl p-4 transition-all hover:border-primary/50 hover:shadow-md"
        >
          <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10 text-primary group-hover:scale-105 transition-transform">
            <UserPlus className="size-5" />
          </div>
          <div>
            <p className="text-sm font-bold text-foreground">Register Patient</p>
            <p className="text-xs text-muted-foreground">New patient file</p>
          </div>
        </Link>

        <Link
          to="/receptionist/appointments"
          search={{ action: "book" }}
          className="surface-card group flex flex-col gap-2 rounded-2xl p-4 transition-all hover:border-primary/50 hover:shadow-md"
        >
          <div className="flex size-10 items-center justify-center rounded-xl bg-blue-500/10 text-blue-600 group-hover:scale-105 transition-transform">
            <CalendarPlus className="size-5" />
          </div>
          <div>
            <p className="text-sm font-bold text-foreground">Book Appointment</p>
            <p className="text-xs text-muted-foreground">Schedule doctor slot</p>
          </div>
        </Link>

        <Link
          to="/receptionist/check-in"
          className="surface-card group flex flex-col gap-2 rounded-2xl p-4 transition-all hover:border-primary/50 hover:shadow-md"
        >
          <div className="flex size-10 items-center justify-center rounded-xl bg-emerald-500/10 text-emerald-600 group-hover:scale-105 transition-transform">
            <CheckCircle2 className="size-5" />
          </div>
          <div>
            <p className="text-sm font-bold text-foreground">Check In Patient</p>
            <p className="text-xs text-muted-foreground">Issue queue token</p>
          </div>
        </Link>

        <Link
          to="/receptionist/queue"
          search={{ action: "walkin" }}
          className="surface-card group flex flex-col gap-2 rounded-2xl p-4 transition-all hover:border-primary/50 hover:shadow-md"
        >
          <div className="flex size-10 items-center justify-center rounded-xl bg-amber-500/10 text-amber-600 group-hover:scale-105 transition-transform">
            <Footprints className="size-5" />
          </div>
          <div>
            <p className="text-sm font-bold text-foreground">Create Walk-In</p>
            <p className="text-xs text-muted-foreground">No prior booking</p>
          </div>
        </Link>

        <Link
          to="/receptionist/queue"
          className="surface-card group flex flex-col gap-2 rounded-2xl p-4 transition-all hover:border-primary/50 hover:shadow-md"
        >
          <div className="flex size-10 items-center justify-center rounded-xl bg-purple-500/10 text-purple-600 group-hover:scale-105 transition-transform">
            <ListOrdered className="size-5" />
          </div>
          <div>
            <p className="text-sm font-bold text-foreground">View Live Queue</p>
            <p className="text-xs text-muted-foreground">Manage OPD order</p>
          </div>
        </Link>
      </div>

      {/* KPI Cards */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          className="surface-card rounded-2xl p-5 shadow-sm border"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
              Today's Appointments
            </span>
            <div className="size-9 rounded-xl bg-primary/10 text-primary grid place-items-center">
              <CalendarDays className="size-4" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-3xl font-extrabold tracking-tight">
              {loadingAppts ? "..." : totalToday}
            </span>
            <span className="text-xs text-muted-foreground">scheduled for today</span>
          </div>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.05 }}
          className="surface-card rounded-2xl p-5 shadow-sm border"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
              Confirmed
            </span>
            <div className="size-9 rounded-xl bg-blue-500/10 text-blue-600 grid place-items-center">
              <CheckCircle2 className="size-4" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-3xl font-extrabold tracking-tight text-blue-600">
              {loadingAppts ? "..." : confirmedCount}
            </span>
            <span className="text-xs text-muted-foreground">ready for check-in</span>
          </div>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="surface-card rounded-2xl p-5 shadow-sm border"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
              In Queue / Waiting
            </span>
            <div className="size-9 rounded-xl bg-amber-500/10 text-amber-600 grid place-items-center">
              <Hourglass className="size-4" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-3xl font-extrabold tracking-tight text-amber-600">
              {loadingQueue ? "..." : waitingInQueue}
            </span>
            <span className="text-xs text-muted-foreground">active OPD tokens (walk-in & checked-in)</span>
          </div>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.15 }}
          className="surface-card rounded-2xl p-5 shadow-sm border"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
              Completed / Cancelled
            </span>
            <div className="size-9 rounded-xl bg-emerald-500/10 text-emerald-600 grid place-items-center">
              <Users className="size-4" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-2xl font-extrabold tracking-tight text-emerald-600">
              {loadingAppts || loadingQueue ? "..." : completedCount}
            </span>
            <span className="text-xs text-muted-foreground">
              done · <span className="text-destructive font-medium">{cancelledCount} cancelled</span>
            </span>
          </div>
        </motion.div>
      </div>

      {/* Main Content Sections: Today's Appointments & Current Queue Preview */}
      <div className="grid gap-6 lg:grid-cols-3">
        {/* Left 2 Cols: Today's Appointments */}
        <div className="lg:col-span-2 space-y-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div>
              <h2 className="text-base font-bold tracking-tight">Today's Appointments</h2>
              <p className="text-xs text-muted-foreground">
                All scheduled consultations for {todayDateStr}
              </p>
            </div>
            <div className="relative w-full sm:w-64">
              <Search className="absolute left-3 top-2.5 size-4 text-muted-foreground" />
              <input
                type="text"
                placeholder="Search patient or doctor..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full rounded-xl border bg-background pl-9 pr-3 py-1.5 text-xs outline-none focus:border-primary focus:ring-1 focus:ring-primary"
              />
            </div>
          </div>

          {loadingAppts ? (
            <div className="surface-card rounded-2xl p-12 text-center border">
              <RefreshCw className="size-6 animate-spin mx-auto text-primary mb-2" />
              <p className="text-sm text-muted-foreground">Loading today's schedule...</p>
            </div>
          ) : errorAppts ? (
            <div className="surface-card rounded-2xl p-8 text-center border border-destructive/20 bg-destructive/5">
              <AlertCircle className="size-6 text-destructive mx-auto mb-2" />
              <p className="text-sm font-semibold text-destructive">Failed to load appointments</p>
              <Button
                variant="outline"
                size="sm"
                className="mt-3 rounded-xl"
                onClick={() => refetchAppts()}
              >
                Retry
              </Button>
            </div>
          ) : filteredAppointments.length === 0 ? (
            <div className="surface-card rounded-2xl p-12 text-center border">
              <CalendarDays className="size-8 text-muted-foreground/40 mx-auto mb-2" />
              <p className="text-sm font-semibold">No advance appointments booked for today</p>
              <p className="text-xs text-muted-foreground mt-1">
                {searchQuery
                  ? "Try matching a different name or number."
                  : "All current OPD patients are walk-in tokens (view in Active OPD Queue on the right)."}
              </p>
              <Link to="/receptionist/appointments" search={{ action: "book" }} className="inline-block mt-4">
                <Button size="sm" className="rounded-xl gap-2">
                  <CalendarPlus className="size-4" /> Book Appointment
                </Button>
              </Link>
            </div>
          ) : (
            <div className="surface-card rounded-2xl border overflow-hidden shadow-sm">
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="border-b bg-muted/40 font-semibold text-muted-foreground">
                    <tr>
                      <th className="p-3">Time</th>
                      <th className="p-3">Patient</th>
                      <th className="p-3">Doctor</th>
                      <th className="p-3">Type</th>
                      <th className="p-3">Status</th>
                      <th className="p-3 text-right">Action</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y">
                    {filteredAppointments.map((appt) => (
                      <tr key={appt.id} className="hover:bg-muted/30 transition-colors">
                        <td className="p-3 font-semibold whitespace-nowrap">
                          <span className="flex items-center gap-1.5">
                            <Clock className="size-3.5 text-muted-foreground" />
                            {appt.startTime?.slice(0, 5) || "09:00"}
                          </span>
                        </td>
                        <td className="p-3">
                          <p className="font-semibold text-foreground">{appt.patientName}</p>
                          <p className="text-[11px] text-muted-foreground font-mono">
                            {appt.appointmentNumber}
                          </p>
                        </td>
                        <td className="p-3">
                          <p className="font-medium text-foreground">{appt.doctorName}</p>
                          <p className="text-[11px] text-muted-foreground">{appt.specialization || "General"}</p>
                        </td>
                        <td className="p-3">
                          <Badge variant="outline" className="text-[10px] font-medium rounded-full">
                            {appt.appointmentType?.replace("_", " ")}
                          </Badge>
                        </td>
                        <td className="p-3">
                          <Badge
                            className={cn(
                              "text-[10px] font-semibold rounded-full border-0",
                              appt.status === "CONFIRMED" && "bg-blue-500/10 text-blue-600",
                              appt.status === "BOOKED" && "bg-amber-500/10 text-amber-600",
                              appt.status === "CHECKED_IN" && "bg-purple-500/10 text-purple-600",
                              appt.status === "COMPLETED" && "bg-emerald-500/10 text-emerald-600",
                              appt.status === "CANCELLED" && "bg-destructive/10 text-destructive",
                            )}
                          >
                            {appt.status}
                          </Badge>
                        </td>
                        <td className="p-3 text-right">
                          {appt.status === "CONFIRMED" || appt.status === "BOOKED" ? (
                            <Link to="/receptionist/check-in" search={{ appointmentId: appt.id }}>
                              <Button size="sm" variant="default" className="h-7 rounded-lg text-xs gap-1">
                                Check In
                              </Button>
                            </Link>
                          ) : (
                            <Link to="/receptionist/appointments">
                              <Button size="sm" variant="ghost" className="h-7 rounded-lg text-xs text-muted-foreground">
                                Details
                              </Button>
                            </Link>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>

        {/* Right Col: Live Queue Snapshot */}
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-base font-bold tracking-tight">Active OPD Queue</h2>
              <p className="text-xs text-muted-foreground">Tokens currently in waiting area</p>
            </div>
            <Link to="/receptionist/queue" className="text-xs font-semibold text-primary hover:underline flex items-center gap-1">
              All <ArrowRight className="size-3" />
            </Link>
          </div>

          {loadingQueue ? (
            <div className="surface-card rounded-2xl p-8 text-center border">
              <RefreshCw className="size-5 animate-spin mx-auto text-primary mb-2" />
              <p className="text-xs text-muted-foreground">Loading queue...</p>
            </div>
          ) : queueEntries.length === 0 ? (
            <div className="surface-card rounded-2xl p-8 text-center border">
              <Hourglass className="size-6 text-muted-foreground/40 mx-auto mb-2" />
              <p className="text-xs font-semibold">No active queue tokens</p>
              <p className="text-[11px] text-muted-foreground mt-1">
                Patients checked in will appear here with token numbers.
              </p>
              <Link to="/receptionist/check-in" className="inline-block mt-3">
                <Button size="sm" variant="outline" className="rounded-xl text-xs gap-1.5">
                  <CheckCircle2 className="size-3.5" /> Check In First Patient
                </Button>
              </Link>
            </div>
          ) : (
            <div className="space-y-2.5">
              {queueEntries.slice(0, 6).map((q) => (
                <div
                  key={q.id}
                  className="surface-card flex items-center justify-between rounded-xl p-3 border hover:border-primary/40 transition-colors"
                >
                  <div className="flex items-center gap-3">
                    <span className="flex size-9 items-center justify-center rounded-xl bg-primary text-primary-foreground font-black text-sm shadow-sm">
                      #{q.tokenNumber}
                    </span>
                    <div>
                      <p className="text-xs font-bold leading-tight">{q.patientName || `Patient #${q.patientId}`}</p>
                      <p className="text-[11px] text-muted-foreground">
                        {q.doctorName || `Doctor #${q.doctorId}`}
                      </p>
                    </div>
                  </div>
                  <Badge
                    variant="outline"
                    className={cn(
                      "text-[10px] font-semibold rounded-full",
                      q.status === "WAITING" && "bg-amber-500/10 text-amber-600 border-amber-300",
                      q.status === "CALLED" && "bg-blue-500/10 text-blue-600 border-blue-300",
                      q.status === "IN_CONSULTATION" && "bg-purple-500/10 text-purple-600 border-purple-300",
                      q.status === "COMPLETED" && "bg-emerald-500/10 text-emerald-600 border-emerald-300",
                    )}
                  >
                    {q.status}
                  </Badge>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
