import { createFileRoute, useSearch, Link } from "@tanstack/react-router";
import { useState, useEffect } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { motion, AnimatePresence } from "motion/react";
import {
  CheckCircle2,
  Search,
  Clock,
  User,
  Stethoscope,
  Ticket,
  Footprints,
  AlertCircle,
  RefreshCw,
  Printer,
  ArrowRight,
  Sparkles,
} from "lucide-react";
import { toast } from "sonner";

import { PageHeader } from "@/components/layout/app-shell";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { appointmentApi, type AppointmentResponse } from "@/services/appointment-api";
import { queueApi, type QueueEntryResponse } from "@/services/queue-api";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/receptionist/check-in")({
  validateSearch: (search: Record<string, unknown>): { appointmentId?: number | undefined } => ({
    appointmentId: search["appointmentId"] ? Number(search["appointmentId"]) : undefined,
  }),
  head: () => ({
    meta: [
      { title: "Patient Check-In — Meridian Health OS" },
      { name: "description", content: "Check in today's patients and issue OPD queue tokens." },
    ],
  }),
  component: ReceptionistCheckInPage,
});

function ReceptionistCheckInPage() {
  const searchParams = useSearch({ from: "/receptionist/check-in" });
  const queryClient = useQueryClient();

  const [search, setSearch] = useState("");
  const [selectedAppointment, setSelectedAppointment] = useState<AppointmentResponse | null>(null);
  const [issuedToken, setIssuedToken] = useState<QueueEntryResponse | null>(null);

  // Fetch today's appointments
  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ["receptionist-today-checkin-list"],
    queryFn: () => appointmentApi.getTodayAppointments(undefined, 0, 100),
  });

  const appointments = data?.content || [];

  // If appointmentId is passed in query params, pre-select it
  useEffect(() => {
    if (searchParams.appointmentId && appointments.length > 0) {
      const match = appointments.find((a) => a.id === searchParams.appointmentId);
      if (match) {
        setSelectedAppointment(match);
      }
    }
  }, [searchParams.appointmentId, appointments]);

  // Check-in mutation
  const checkInMutation = useMutation({
    mutationFn: (appointmentId: number) => queueApi.checkIn(appointmentId),
    onSuccess: (queueEntry) => {
      setIssuedToken(queueEntry);
      toast.success(`Check-in successful! Token #${queueEntry.tokenNumber} issued.`);
      queryClient.invalidateQueries({ queryKey: ["receptionist-today-checkin-list"] });
      queryClient.invalidateQueries({ queryKey: ["receptionist-today-queue"] });
    },
    onError: (err: any) => {
      const msg = err.message || "Check-in failed";
      if (msg.toLowerCase().includes("active") || msg.toLowerCase().includes("duplicate")) {
        toast.error("Patient already has an active queue token for this doctor today!");
      } else {
        toast.error(msg);
      }
    },
  });

  const handleCheckIn = () => {
    if (!selectedAppointment) return;
    checkInMutation.mutate(selectedAppointment.id);
  };

  // Filter appointments for check-in: show all today, highlight un-checked in
  const filtered = appointments.filter((a) => {
    if (!search.trim()) return true;
    const q = search.toLowerCase();
    return (
      a.patientName?.toLowerCase().includes(q) ||
      a.doctorName?.toLowerCase().includes(q) ||
      a.appointmentNumber?.toLowerCase().includes(q)
    );
  });

  return (
    <div className="space-y-6 p-4 sm:p-6 lg:p-8">
      <PageHeader
        title="Patient Check-In Desk"
        subtitle="Confirm patient arrival for today's appointment and dispatch live OPD queue token."
        actions={
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              className="rounded-xl gap-2"
              onClick={() => refetch()}
            >
              <RefreshCw className="size-3.5" /> Refresh
            </Button>
            <Link to="/receptionist/queue" search={{ action: "walkin" }}>
              <Button size="sm" variant="secondary" className="rounded-xl gap-2">
                <Footprints className="size-4" /> Create Walk-In
              </Button>
            </Link>
          </div>
        }
      />

      <div className="grid gap-6 lg:grid-cols-12">
        {/* Left Column: Today's Appointments Lookup (7 cols) */}
        <div className="lg:col-span-7 space-y-4">
          <div className="surface-card rounded-2xl p-4 border shadow-sm space-y-3">
            <div className="relative">
              <Search className="absolute left-3 top-3 size-4 text-muted-foreground" />
              <Input
                placeholder="Search today's patient by name, mobile, or appointment #..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="pl-9 rounded-xl"
              />
            </div>
            <div className="flex items-center justify-between text-xs text-muted-foreground">
              <span>Today's scheduled appointments: {appointments.length}</span>
              <span>Click any record to check in</span>
            </div>
          </div>

          {isLoading ? (
            <div className="surface-card rounded-2xl p-16 text-center border">
              <RefreshCw className="size-8 animate-spin mx-auto text-primary mb-3" />
              <p className="text-sm font-semibold">Loading today's schedule...</p>
            </div>
          ) : isError ? (
            <div className="surface-card rounded-2xl p-12 text-center border border-destructive/20 bg-destructive/5">
              <AlertCircle className="size-8 text-destructive mx-auto mb-3" />
              <p className="text-sm font-semibold text-destructive">Failed to load appointments</p>
              <Button variant="outline" size="sm" className="mt-3 rounded-xl" onClick={() => refetch()}>
                Retry
              </Button>
            </div>
          ) : filtered.length === 0 ? (
            <div className="surface-card rounded-2xl p-16 text-center border">
              <Clock className="size-10 text-muted-foreground/30 mx-auto mb-3" />
              <h3 className="text-base font-bold">No appointments match</h3>
              <p className="text-xs text-muted-foreground mt-1 max-w-sm mx-auto">
                No matching appointment found for today. For patients arriving without an appointment, use Walk-In.
              </p>
              <Link to="/receptionist/queue" search={{ action: "walkin" }} className="inline-block mt-4">
                <Button size="sm" className="rounded-xl gap-2">
                  <Footprints className="size-4" /> Issue Walk-In Token
                </Button>
              </Link>
            </div>
          ) : (
            <div className="space-y-2.5">
              {filtered.map((appt) => {
                const isSelected = selectedAppointment?.id === appt.id;
                const isAlreadyCheckedIn = appt.status === "CHECKED_IN";
                const isCancelled = appt.status === "CANCELLED";
                const isCompleted = appt.status === "COMPLETED";

                return (
                  <div
                    key={appt.id}
                    onClick={() => {
                      if (!isCancelled && !isCompleted) {
                        setSelectedAppointment(appt);
                        setIssuedToken(null);
                      }
                    }}
                    className={cn(
                      "surface-card rounded-2xl p-4 border transition-all cursor-pointer flex flex-col sm:flex-row sm:items-center justify-between gap-3",
                      isSelected && "border-primary ring-2 ring-primary/20 bg-primary/5",
                      !isSelected && "hover:border-primary/40",
                      (isCancelled || isCompleted) && "opacity-50 cursor-not-allowed",
                    )}
                  >
                    <div className="flex items-start gap-3.5">
                      <div className="size-10 rounded-xl bg-primary/10 text-primary grid place-items-center font-bold text-sm shrink-0 mt-0.5">
                        <Clock className="size-4" />
                      </div>
                      <div>
                        <div className="flex items-center gap-2">
                          <p className="text-sm font-bold text-foreground">{appt.patientName}</p>
                          <Badge
                            className={cn(
                              "text-[10px] rounded-full border-0 font-semibold",
                              appt.status === "CONFIRMED" && "bg-blue-500/10 text-blue-600",
                              appt.status === "BOOKED" && "bg-amber-500/10 text-amber-600",
                              appt.status === "CHECKED_IN" && "bg-purple-500/10 text-purple-600",
                              appt.status === "COMPLETED" && "bg-emerald-500/10 text-emerald-600",
                              appt.status === "CANCELLED" && "bg-destructive/10 text-destructive",
                            )}
                          >
                            {appt.status}
                          </Badge>
                        </div>
                        <p className="text-xs text-muted-foreground mt-0.5 font-mono">
                          {appt.appointmentNumber} · {appt.startTime?.slice(0, 5)} - {appt.endTime?.slice(0, 5)}
                        </p>
                        <p className="text-xs text-muted-foreground mt-1 flex items-center gap-1.5">
                          <Stethoscope className="size-3 text-primary" /> Dr. {appt.doctorName} ({appt.specialization || "General"})
                        </p>
                      </div>
                    </div>

                    <div className="flex items-center gap-2 self-end sm:self-center">
                      {isAlreadyCheckedIn ? (
                        <Badge variant="outline" className="text-purple-600 border-purple-300 bg-purple-50/50">
                          Already in Queue
                        </Badge>
                      ) : isCancelled ? (
                        <Badge variant="outline" className="text-destructive border-destructive/30">
                          Cancelled
                        </Badge>
                      ) : isCompleted ? (
                        <Badge variant="outline" className="text-emerald-600 border-emerald-300">
                          Finished
                        </Badge>
                      ) : (
                        <Button
                          size="sm"
                          variant={isSelected ? "default" : "outline"}
                          className="rounded-xl text-xs"
                          onClick={(e) => {
                            e.stopPropagation();
                            setSelectedAppointment(appt);
                            setIssuedToken(null);
                          }}
                        >
                          Select
                        </Button>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* Right Column: Check-In Action Card & Live Token Display (5 cols) */}
        <div className="lg:col-span-5 space-y-4">
          <AnimatePresence mode="wait">
            {issuedToken ? (
              /* Success Confirmation Card */
              <motion.div
                key="token-card"
                initial={{ opacity: 0, scale: 0.95 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.95 }}
                className="surface-card rounded-2xl p-6 border-2 border-emerald-500/40 bg-emerald-500/5 shadow-lg text-center space-y-4"
              >
                <div className="size-16 rounded-full bg-emerald-500 text-white grid place-items-center mx-auto shadow-md">
                  <CheckCircle2 className="size-9" />
                </div>

                <div>
                  <Badge className="bg-emerald-600 text-white border-0 text-xs px-3 py-0.5 rounded-full mb-2">
                    CHECKED IN SUCCESSFULLY
                  </Badge>
                  <h3 className="text-lg font-extrabold text-foreground">OPD Queue Token</h3>
                  <p className="text-xs text-muted-foreground mt-0.5">
                    City Central Hospital · OPD Desk
                  </p>
                </div>

                <div className="surface-card rounded-2xl p-5 border bg-background shadow-inner">
                  <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wider block">
                    Assigned Token Number
                  </span>
                  <div className="text-5xl font-black text-primary my-2 tracking-tight">
                    #{issuedToken.tokenNumber}
                  </div>
                  <div className="border-t pt-3 mt-3 text-xs space-y-1.5 text-left">
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Patient:</span>
                      <span className="font-bold text-foreground">
                        {issuedToken.patientName || selectedAppointment?.patientName}
                      </span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Doctor:</span>
                      <span className="font-semibold text-foreground">
                        Dr. {issuedToken.doctorName || selectedAppointment?.doctorName}
                      </span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Priority:</span>
                      <Badge variant="outline" className="text-[10px] rounded-full">
                        {issuedToken.priority}
                      </Badge>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Check-in Time:</span>
                      <span className="font-mono text-muted-foreground">
                        {new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
                      </span>
                    </div>
                  </div>
                </div>

                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    className="flex-1 rounded-xl text-xs gap-1.5"
                    onClick={() => {
                      setSelectedAppointment(null);
                      setIssuedToken(null);
                    }}
                  >
                    Check In Next
                  </Button>
                  <Link to="/receptionist/queue" className="flex-1">
                    <Button className="w-full rounded-xl text-xs gap-1.5 bg-primary">
                      View Queue <ArrowRight className="size-3.5" />
                    </Button>
                  </Link>
                </div>
              </motion.div>
            ) : selectedAppointment ? (
              /* Selected Appointment Ready to Check-In */
              <motion.div
                key="confirm-card"
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: 10 }}
                className="surface-card rounded-2xl p-6 border shadow-md space-y-4"
              >
                <div className="flex items-center gap-2 text-primary font-bold text-sm">
                  <Ticket className="size-4" /> Ready to Check In
                </div>

                <div className="surface-card rounded-xl p-4 border bg-muted/20 space-y-2.5 text-xs">
                  <div>
                    <span className="text-muted-foreground">Patient</span>
                    <p className="font-bold text-foreground text-sm">
                      {selectedAppointment.patientName}
                    </p>
                    <p className="text-[11px] text-muted-foreground font-mono">
                      Patient ID #{selectedAppointment.patientId}
                    </p>
                  </div>

                  <div className="border-t pt-2">
                    <span className="text-muted-foreground">Attending Physician</span>
                    <p className="font-semibold text-foreground">
                      Dr. {selectedAppointment.doctorName}
                    </p>
                    <p className="text-[11px] text-muted-foreground">
                      {selectedAppointment.specialization || "General Medicine"}
                    </p>
                  </div>

                  <div className="border-t pt-2 grid grid-cols-2 gap-2">
                    <div>
                      <span className="text-muted-foreground">Appointment #</span>
                      <p className="font-mono font-semibold text-foreground">
                        {selectedAppointment.appointmentNumber}
                      </p>
                    </div>
                    <div>
                      <span className="text-muted-foreground">Scheduled Time</span>
                      <p className="font-semibold text-foreground">
                        {selectedAppointment.startTime?.slice(0, 5)} - {selectedAppointment.endTime?.slice(0, 5)}
                      </p>
                    </div>
                  </div>
                </div>

                <Button
                  size="lg"
                  className="w-full rounded-xl font-bold gap-2 text-sm bg-emerald-600 hover:bg-emerald-700 text-white shadow-md"
                  onClick={handleCheckIn}
                  disabled={checkInMutation.isPending}
                >
                  {checkInMutation.isPending ? (
                    <>
                      <RefreshCw className="size-4 animate-spin" /> Issuing Token...
                    </>
                  ) : (
                    <>
                      <CheckCircle2 className="size-5" /> Generate Queue Token
                    </>
                  )}
                </Button>

                <p className="text-[11px] text-center text-muted-foreground">
                  Generates an active token in the doctor's live queue and notifies the doctor.
                </p>
              </motion.div>
            ) : (
              /* Idle state: instructions */
              <motion.div
                key="idle-card"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                className="surface-card rounded-2xl p-8 border text-center space-y-3"
              >
                <div className="size-14 rounded-2xl bg-primary/10 text-primary grid place-items-center mx-auto">
                  <CheckCircle2 className="size-7" />
                </div>
                <h3 className="text-base font-bold text-foreground">Select an Appointment</h3>
                <p className="text-xs text-muted-foreground leading-relaxed">
                  Choose a patient from today's list on the left to verify details and generate their official OPD queue token number.
                </p>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>
    </div>
  );
}
