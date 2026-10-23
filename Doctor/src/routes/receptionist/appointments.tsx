import { createFileRoute, useSearch, Link } from "@tanstack/react-router";
import { useState, useEffect } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  CalendarDays,
  Clock,
  Search,
  Plus,
  RefreshCw,
  AlertCircle,
  CheckCircle2,
  CalendarClock,
  Ban,
  Filter,
  User,
  Stethoscope,
  ChevronLeft,
  ChevronRight,
  Check,
  X,
  Video,
} from "lucide-react";
import { toast } from "sonner";

import { PageHeader } from "@/components/layout/app-shell";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import {
  appointmentApi,
  type AppointmentResponse,
  type AppointmentStatus,
  type AppointmentType,
  type CreateAppointmentRequest,
  type RescheduleAppointmentRequest,
} from "@/services/appointment-api";
import { patientApi } from "@/services/patient-api";
import { doctorApi, getDoctorFullName } from "@/services/doctor-api";
import { queueApi } from "@/services/queue-api";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/receptionist/appointments")({
  validateSearch: (search: Record<string, unknown>): { action?: string | undefined } => ({
    action: typeof search["action"] === "string" ? (search["action"] as string) : undefined,
  }),
  head: () => ({
    meta: [
      { title: "Appointments Desk — Meridian Health OS" },
      {
        name: "description",
        content: "Schedule, confirm, reschedule, and manage OPD appointments.",
      },
    ],
  }),
  component: ReceptionistAppointmentsPage,
});

function getDeterministicLocalDate(d = new Date()) {
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function ReceptionistAppointmentsPage() {
  const searchParams = useSearch({ from: "/receptionist/appointments" });
  const queryClient = useQueryClient();

  const todayStr: string = getDeterministicLocalDate();
  const [selectedDate, setSelectedDate] = useState<string>(todayStr);
  const [statusFilter, setStatusFilter] = useState<string>("ALL");
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);

  // Modals state
  const [bookModalOpen, setBookModalOpen] = useState(false);
  const [rescheduleItem, setRescheduleItem] = useState<AppointmentResponse | null>(null);
  const [cancelItem, setCancelItem] = useState<AppointmentResponse | null>(null);
  const [cancelReason, setCancelReason] = useState("");
  const [newRescheduleDate, setNewRescheduleDate] = useState(todayStr);
  const [newRescheduleTime, setNewRescheduleTime] = useState("10:00:00");

  useEffect(() => {
    if (searchParams.action === "book") {
      setBookModalOpen(true);
    }
  }, [searchParams.action]);

  // Query appointments
  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ["receptionist-appointments", selectedDate, statusFilter, search, page],
    queryFn: () =>
      appointmentApi.getAppointments({
        appointmentDate: selectedDate || undefined,
        status: statusFilter !== "ALL" ? (statusFilter as AppointmentStatus) : undefined,
        search: search.trim() || undefined,
        page,
        size: 15,
      }),
  });

  // Query doctors for dropdown
  const { data: doctorsData } = useQuery({
    queryKey: ["doctors-list"],
    queryFn: () => doctorApi.getDoctors(),
  });
  const doctors = doctorsData?.content || [];

  // Patient live search for booking
  const [patientSearch, setPatientSearch] = useState("");
  const [selectedPatientId, setSelectedPatientId] = useState<number | null>(null);
  const [selectedDoctorId, setSelectedDoctorId] = useState<number | null>(null);
  const [bookDate, setBookDate] = useState(todayStr);
  const [bookTime, setBookTime] = useState("");
  const [bookType, setBookType] = useState<AppointmentType>("NEW_CONSULTATION");
  const [bookReason, setBookReason] = useState("");
  const [formError, setFormError] = useState<string | null>(null);

  const { data: patientSearchResult } = useQuery({
    queryKey: ["patient-search-booking", patientSearch],
    queryFn: () => patientApi.getPatients({ search: patientSearch, size: 5 }),
    enabled: patientSearch.length >= 2,
  });

  // Doctor availability slots for selected doctor & date
  const { data: availabilityData, isLoading: isLoadingSlots } = useQuery({
    queryKey: ["doctor-avail-slots", selectedDoctorId, bookDate],
    queryFn: () => doctorApi.getDoctorAvailability(selectedDoctorId!, bookDate),
    enabled: Boolean(selectedDoctorId && bookDate),
  });

  // Automatically select the first available slot when doctor availability loads
  useEffect(() => {
    if (availabilityData?.slots && availabilityData.slots.length > 0) {
      const isCurrentSlotAvailable = availabilityData.slots.some(
        (s) => s.startTime === bookTime && s.available,
      );
      if (!isCurrentSlotAvailable) {
        const firstAvail = availabilityData.slots.find((s) => s.available);
        if (firstAvail) {
          setBookTime(firstAvail.startTime);
        } else {
          setBookTime("");
        }
      }
    }
  }, [availabilityData, bookTime]);

  // Mutations
  const bookMutation = useMutation({
    mutationFn: (req: CreateAppointmentRequest) => appointmentApi.createAppointment(req),
    onSuccess: () => {
      toast.success("Appointment booked successfully!");
      setFormError(null);
      queryClient.invalidateQueries({ queryKey: ["receptionist-appointments"] });
      queryClient.invalidateQueries({ queryKey: ["doctor-avail-slots"] });
      setBookModalOpen(false);
      resetBookingForm();
    },
    onError: (err: unknown) => {
      const msg = err instanceof Error ? err.message : "Failed to book appointment";
      setFormError(msg);
      toast.error(msg);
    },
  });

  const confirmMutation = useMutation({
    mutationFn: (id: number) => appointmentApi.confirmAppointment(id),
    onSuccess: () => {
      toast.success("Appointment confirmed!");
      queryClient.invalidateQueries({ queryKey: ["receptionist-appointments"] });
    },
    onError: (err: unknown) => {
      const msg = err instanceof Error ? err.message : "Failed to confirm appointment";
      toast.error(msg);
    },
  });

  const rescheduleMutation = useMutation({
    mutationFn: ({ id, req }: { id: number; req: RescheduleAppointmentRequest }) =>
      appointmentApi.rescheduleAppointment(id, req),
    onSuccess: () => {
      toast.success("Appointment rescheduled successfully!");
      queryClient.invalidateQueries({ queryKey: ["receptionist-appointments"] });
      setRescheduleItem(null);
    },
    onError: (err: unknown) => {
      const msg = err instanceof Error ? err.message : "Failed to reschedule appointment";
      toast.error(msg);
    },
  });

  const cancelMutation = useMutation({
    mutationFn: ({ id, reason }: { id: number; reason?: string }) =>
      appointmentApi.cancelAppointment(id, reason),
    onSuccess: () => {
      toast.success("Appointment cancelled");
      queryClient.invalidateQueries({ queryKey: ["receptionist-appointments"] });
      setCancelItem(null);
      setCancelReason("");
    },
    onError: (err: unknown) => {
      const msg = err instanceof Error ? err.message : "Failed to cancel appointment";
      toast.error(msg);
    },
  });

  const checkInMutation = useMutation({
    mutationFn: (id: number) => queueApi.checkIn(id),
    onSuccess: (queueEntry) => {
      toast.success(`Patient checked in! Token #${queueEntry.tokenNumber}`);
      queryClient.invalidateQueries({ queryKey: ["receptionist-appointments"] });
      queryClient.invalidateQueries({ queryKey: ["receptionist-today-queue"] });
    },
    onError: (err: unknown) => {
      const msg = err instanceof Error ? err.message : "Check-in failed. Please verify status.";
      toast.error(msg);
    },
  });

  const resetBookingForm = () => {
    setSelectedPatientId(null);
    setPatientSearch("");
    setSelectedDoctorId(null);
    setBookReason("");
    setBookTime("");
    setBookType("NEW_CONSULTATION");
    setFormError(null);
  };

  const handleBookSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    if (!selectedPatientId) {
      const err = "Please search and select a patient from the dropdown.";
      setFormError(err);
      toast.error(err);
      return;
    }
    if (!selectedDoctorId) {
      const err = "Please select an attending doctor.";
      setFormError(err);
      toast.error(err);
      return;
    }
    if (!bookDate) {
      const err = "Please select an appointment date.";
      setFormError(err);
      toast.error(err);
      return;
    }
    if (!bookTime) {
      const err = "Please select an available consultation slot.";
      setFormError(err);
      toast.error(err);
      return;
    }

    // Ensure time formatted HH:mm:ss
    const formattedTime = bookTime.length === 5 ? `${bookTime}:00` : bookTime;
    bookMutation.mutate({
      patientId: selectedPatientId,
      doctorId: selectedDoctorId,
      appointmentDate: bookDate,
      startTime: formattedTime,
      appointmentType: bookType,
      reason: bookReason || "General OPD Checkup",
    });
  };

  const appointments = data?.content || [];
  const totalPages = data?.totalPages || 0;
  const totalElements = data?.totalElements || 0;

  return (
    <div className="space-y-6 p-4 sm:p-6 lg:p-8">
      <PageHeader
        title="Appointments Desk"
        subtitle="Manage daily OPD schedules, book new visits, reschedule, or cancel bookings."
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
            <Button size="sm" className="rounded-xl gap-2" onClick={() => setBookModalOpen(true)}>
              <Plus className="size-4" /> Book Appointment
            </Button>
          </div>
        }
      />

      {/* Filter and Search Bar */}
      <div className="surface-card rounded-2xl p-4 border shadow-sm flex flex-col md:flex-row gap-3 items-center justify-between">
        <div className="flex flex-wrap items-center gap-3 w-full md:w-auto">
          {/* Date Picker */}
          <div className="flex items-center gap-2">
            <Label
              htmlFor="date-filter"
              className="text-xs text-muted-foreground whitespace-nowrap"
            >
              Date:
            </Label>
            <Input
              id="date-filter"
              type="date"
              value={selectedDate}
              onChange={(e) => {
                setSelectedDate(e.target.value);
                setPage(0);
              }}
              className="h-8 rounded-xl text-xs w-36"
            />
            {selectedDate && (
              <Button
                variant="ghost"
                size="sm"
                className="h-8 px-2 text-xs"
                onClick={() => setSelectedDate("")}
              >
                All Dates
              </Button>
            )}
          </div>

          {/* Status Filter */}
          <div className="flex items-center gap-2">
            <Label
              htmlFor="status-filter"
              className="text-xs text-muted-foreground whitespace-nowrap"
            >
              Status:
            </Label>
            <select
              id="status-filter"
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setPage(0);
              }}
              className="h-8 rounded-xl border bg-background px-2.5 text-xs outline-none focus:border-primary"
            >
              <option value="ALL">All Statuses</option>
              <option value="BOOKED">Booked</option>
              <option value="CONFIRMED">Confirmed</option>
              <option value="CHECKED_IN">Checked In</option>
              <option value="COMPLETED">Completed</option>
              <option value="CANCELLED">Cancelled</option>
            </select>
          </div>
        </div>

        {/* Search Field */}
        <div className="relative w-full md:w-64">
          <Search className="absolute left-3 top-2.5 size-4 text-muted-foreground" />
          <Input
            placeholder="Search patient or doctor..."
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(0);
            }}
            className="pl-9 h-8 rounded-xl text-xs"
          />
        </div>
      </div>

      {/* Appointments List */}
      {isLoading ? (
        <div className="surface-card rounded-2xl p-16 text-center border">
          <RefreshCw className="size-8 animate-spin mx-auto text-primary mb-3" />
          <p className="text-sm font-semibold">Loading appointments...</p>
        </div>
      ) : isError ? (
        <div className="surface-card rounded-2xl p-12 text-center border border-destructive/20 bg-destructive/5">
          <AlertCircle className="size-8 text-destructive mx-auto mb-3" />
          <p className="text-sm font-semibold text-destructive">Failed to fetch appointments</p>
          <Button variant="outline" size="sm" className="mt-3 rounded-xl" onClick={() => refetch()}>
            Retry
          </Button>
        </div>
      ) : appointments.length === 0 ? (
        <div className="surface-card rounded-2xl p-16 text-center border">
          <CalendarDays className="size-10 text-muted-foreground/30 mx-auto mb-3" />
          <h3 className="text-base font-bold">No appointments found</h3>
          <p className="text-xs text-muted-foreground mt-1 max-w-sm mx-auto">
            {selectedDate
              ? `No appointments scheduled for ${selectedDate}.`
              : "No appointments match your filters."}
          </p>
          <Button
            size="sm"
            className="mt-4 rounded-xl gap-2"
            onClick={() => setBookModalOpen(true)}
          >
            <Plus className="size-4" /> Book First Appointment
          </Button>
        </div>
      ) : (
        <div className="surface-card rounded-2xl border overflow-hidden shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="border-b bg-muted/40 font-semibold text-muted-foreground">
                <tr>
                  <th className="p-3.5">Appt #</th>
                  <th className="p-3.5">Schedule</th>
                  <th className="p-3.5">Patient Details</th>
                  <th className="p-3.5">Doctor</th>
                  <th className="p-3.5">Type & Reason</th>
                  <th className="p-3.5">Status</th>
                  <th className="p-3.5 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {appointments.map((appt) => (
                  <tr key={appt.id} className="hover:bg-muted/30 transition-colors">
                    <td className="p-3.5 font-mono font-semibold text-primary">
                      {appt.appointmentNumber}
                    </td>
                    <td className="p-3.5 whitespace-nowrap">
                      <p className="font-semibold text-foreground flex items-center gap-1.5">
                        <Clock className="size-3.5 text-muted-foreground" />
                        {appt.startTime?.slice(0, 5)} - {appt.endTime?.slice(0, 5)}
                      </p>
                      <p className="text-[11px] text-muted-foreground mt-0.5">
                        {appt.appointmentDate}
                      </p>
                    </td>
                    <td className="p-3.5">
                      <p className="font-bold text-foreground text-sm">{appt.patientName}</p>
                      <p className="text-[11px] text-muted-foreground">ID #{appt.patientId}</p>
                    </td>
                    <td className="p-3.5">
                      <p className="font-medium text-foreground">{appt.doctorName}</p>
                      <p className="text-[11px] text-muted-foreground">
                        {appt.specialization || "General Medicine"}
                      </p>
                    </td>
                    <td className="p-3.5 max-w-xs">
                      <Badge
                        variant="outline"
                        className={cn(
                          "text-[10px] rounded-full inline-flex items-center gap-1",
                          appt.appointmentType === "TELECONSULTATION" &&
                            "border-primary/40 bg-primary/10 text-primary font-medium",
                        )}
                      >
                        {appt.appointmentType === "TELECONSULTATION" && (
                          <Video className="size-3" />
                        )}
                        {appt.appointmentType === "TELECONSULTATION"
                          ? "Teleconsultation"
                          : appt.appointmentType?.replace("_", " ")}
                      </Badge>
                      <p className="text-[11px] text-muted-foreground truncate mt-1">
                        {appt.reason || "General Consultation"}
                      </p>
                    </td>
                    <td className="p-3.5">
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
                    <td className="p-3.5 text-right whitespace-nowrap">
                      <div className="flex items-center justify-end gap-1.5">
                        {/* Quick Check In if booked or confirmed */}
                        {(appt.status === "CONFIRMED" || appt.status === "BOOKED") && (
                          <Button
                            size="sm"
                            className="rounded-lg h-7 text-xs gap-1 bg-emerald-600 hover:bg-emerald-700 text-white"
                            onClick={() => checkInMutation.mutate(appt.id)}
                            disabled={checkInMutation.isPending}
                          >
                            <CheckCircle2 className="size-3.5" /> Check In
                          </Button>
                        )}

                        {/* Confirm Button if only booked */}
                        {appt.status === "BOOKED" && (
                          <Button
                            size="sm"
                            variant="outline"
                            className="rounded-lg h-7 text-xs text-blue-600 border-blue-200 hover:bg-blue-50"
                            onClick={() => confirmMutation.mutate(appt.id)}
                            disabled={confirmMutation.isPending}
                          >
                            Confirm
                          </Button>
                        )}

                        {/* Reschedule Button */}
                        {appt.status !== "COMPLETED" && appt.status !== "CANCELLED" && (
                          <Button
                            size="sm"
                            variant="ghost"
                            className="rounded-lg h-7 text-xs text-muted-foreground hover:text-foreground"
                            onClick={() => {
                              setRescheduleItem(appt);
                              setNewRescheduleDate(appt.appointmentDate);
                              setNewRescheduleTime(appt.startTime);
                            }}
                          >
                            Reschedule
                          </Button>
                        )}

                        {/* Cancel Button */}
                        {appt.status !== "COMPLETED" && appt.status !== "CANCELLED" && (
                          <Button
                            size="sm"
                            variant="ghost"
                            className="rounded-lg h-7 text-xs text-destructive hover:bg-destructive/10"
                            onClick={() => setCancelItem(appt)}
                          >
                            Cancel
                          </Button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between border-t px-4 py-3">
              <span className="text-xs text-muted-foreground">
                Page {page + 1} of {totalPages}
              </span>
              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page === 0}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  className="rounded-xl h-8 px-3"
                >
                  <ChevronLeft className="size-3.5 mr-1" /> Prev
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page >= totalPages - 1}
                  onClick={() => setPage((p) => p + 1)}
                  className="rounded-xl h-8 px-3"
                >
                  Next <ChevronRight className="size-3.5 ml-1" />
                </Button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Book Appointment Modal */}
      <Dialog open={bookModalOpen} onOpenChange={setBookModalOpen}>
        <DialogContent className="max-w-xl max-h-[90vh] overflow-y-auto rounded-2xl">
          <DialogHeader>
            <DialogTitle className="text-xl font-bold flex items-center gap-2">
              <CalendarDays className="size-5 text-primary" /> Book OPD Appointment
            </DialogTitle>
            <DialogDescription>
              Select patient, attending physician, and available consultation slot.
            </DialogDescription>
          </DialogHeader>

          <form onSubmit={handleBookSubmit} className="space-y-4 pt-2">
            {/* Patient Search & Select */}
            <div className="space-y-1.5">
              <Label>Select Patient *</Label>
              {selectedPatientId ? (
                <div className="surface-card rounded-xl p-3 border flex items-center justify-between">
                  <div className="flex items-center gap-2.5">
                    <User className="size-4 text-primary" />
                    <div>
                      <p className="text-xs font-bold text-foreground">
                        Selected Patient ID: #{selectedPatientId}
                      </p>
                      <p className="text-[11px] text-muted-foreground">{patientSearch}</p>
                    </div>
                  </div>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    className="h-7 text-xs"
                    onClick={() => {
                      setSelectedPatientId(null);
                      setPatientSearch("");
                    }}
                  >
                    Change
                  </Button>
                </div>
              ) : (
                <div className="space-y-2">
                  <Input
                    placeholder="Type patient name, phone, or code to search..."
                    value={patientSearch}
                    onChange={(e) => setPatientSearch(e.target.value)}
                    className="rounded-xl text-sm"
                  />
                  {patientSearchResult && patientSearchResult.content.length > 0 && (
                    <div className="rounded-xl border bg-popover p-1 shadow-md space-y-1">
                      {patientSearchResult.content.map((p) => (
                        <button
                          key={p.id}
                          type="button"
                          className="w-full text-left p-2 rounded-lg hover:bg-muted flex items-center justify-between text-xs"
                          onClick={() => {
                            setSelectedPatientId(p.id);
                            setPatientSearch(`${p.firstName} ${p.lastName} (${p.patientCode})`);
                          }}
                        >
                          <div>
                            <span className="font-bold">
                              {p.firstName} {p.lastName}
                            </span>
                            <span className="text-muted-foreground font-mono ml-2">
                              {p.patientCode}
                            </span>
                          </div>
                          <span className="text-muted-foreground">{p.mobileNumber}</span>
                        </button>
                      ))}
                    </div>
                  )}
                  <p className="text-[11px] text-muted-foreground">
                    Don't see patient?{" "}
                    <Link
                      to="/receptionist/patients"
                      search={{ action: "register" }}
                      className="text-primary font-semibold hover:underline"
                    >
                      Register them first
                    </Link>
                  </p>
                </div>
              )}
            </div>

            {/* Doctor Select */}
            <div className="space-y-1.5">
              <Label htmlFor="doctorSelect">Attending Doctor *</Label>
              <select
                id="doctorSelect"
                required
                value={selectedDoctorId || ""}
                onChange={(e) => {
                  setSelectedDoctorId(Number(e.target.value) || null);
                  setFormError(null);
                }}
                className="w-full rounded-xl border bg-background px-3 py-2 text-sm outline-none focus:border-primary"
              >
                <option value="">Select a Doctor</option>
                {doctors.map((doc) => (
                  <option key={doc.id} value={doc.id}>
                    Dr. {getDoctorFullName(doc)} ({doc.specialization || "General"} · Fee ₹
                    {doc.consultationFee ?? 0})
                  </option>
                ))}
              </select>
            </div>

            {/* Date & Slot */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label htmlFor="bookDate">Appointment Date *</Label>
                <Input
                  id="bookDate"
                  type="date"
                  required
                  min={todayStr}
                  value={bookDate}
                  onChange={(e) => {
                    setBookDate(e.target.value);
                    setFormError(null);
                  }}
                  className="rounded-xl"
                />
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="bookTime">Slot Time (Start) *</Label>
                {isLoadingSlots ? (
                  <div className="flex items-center gap-2 h-9 px-3 rounded-xl border bg-muted/40 text-xs text-muted-foreground">
                    <RefreshCw className="size-3 animate-spin" /> Checking available slots...
                  </div>
                ) : availabilityData?.slots && availabilityData.slots.length > 0 ? (
                  <>
                    <select
                      id="bookTime"
                      value={bookTime}
                      onChange={(e) => {
                        setBookTime(e.target.value);
                        setFormError(null);
                      }}
                      className="w-full rounded-xl border bg-background px-3 py-2 text-sm outline-none focus:border-primary"
                    >
                      {availabilityData.slots.map((s, idx) => (
                        <option key={idx} value={s.startTime} disabled={!s.available}>
                          {s.startTime.slice(0, 5)} - {s.endTime.slice(0, 5)}{" "}
                          {s.available ? "(Available)" : "(Booked)"}
                        </option>
                      ))}
                    </select>
                    {!availabilityData.slots.some((s) => s.available) && (
                      <p className="text-[11px] text-amber-600 font-medium">
                        All consultation slots for this date are fully booked.
                      </p>
                    )}
                  </>
                ) : selectedDoctorId ? (
                  <div className="space-y-1">
                    <Input
                      id="bookTime"
                      type="time"
                      required
                      value={bookTime.slice(0, 5)}
                      onChange={(e) => {
                        setBookTime(`${e.target.value}:00`);
                        setFormError(null);
                      }}
                      className="rounded-xl"
                    />
                    <p className="text-[11px] text-muted-foreground">
                      No predefined schedule found. Enter slot time manually.
                    </p>
                  </div>
                ) : (
                  <div className="h-9 px-3 rounded-xl border bg-muted/30 flex items-center text-xs text-muted-foreground">
                    Select a doctor first
                  </div>
                )}
              </div>
            </div>

            {/* Type & Reason */}
            <div className="space-y-1.5">
              <Label htmlFor="bookType">Appointment Type</Label>
              <select
                id="bookType"
                value={bookType}
                onChange={(e) => {
                  setBookType(e.target.value as AppointmentType);
                  setFormError(null);
                }}
                className="w-full rounded-xl border bg-background px-3 py-2 text-sm outline-none focus:border-primary"
              >
                <option value="NEW_CONSULTATION">New Consultation</option>
                <option value="FOLLOW_UP">Follow Up</option>
                <option value="EMERGENCY">Emergency</option>
                <option value="TELECONSULTATION">Teleconsultation</option>
              </select>
              {bookType === "TELECONSULTATION" && (
                <div className="flex items-center gap-1.5 rounded-lg bg-primary/10 border border-primary/20 px-2.5 py-1.5 text-xs text-primary font-medium mt-1.5">
                  <Video className="size-3.5 shrink-0" />
                  <span>
                    Secure video consultation meeting will be prepared for doctor & patient.
                  </span>
                </div>
              )}
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="bookReason">Reason / Chief Complaint</Label>
              <Input
                id="bookReason"
                placeholder="e.g. Fever, headache, regular checkup"
                value={bookReason}
                onChange={(e) => setBookReason(e.target.value)}
                className="rounded-xl"
              />
            </div>

            {/* Inline Error Banner */}
            {formError && (
              <div className="rounded-xl border border-destructive/20 bg-destructive/10 p-3 text-xs text-destructive flex items-start gap-2">
                <AlertCircle className="size-4 shrink-0 mt-0.5" />
                <div className="flex-1 font-medium">{formError}</div>
              </div>
            )}

            <DialogFooter className="pt-3 flex gap-2 justify-end">
              <Button
                type="button"
                variant="outline"
                className="rounded-xl"
                onClick={() => setBookModalOpen(false)}
              >
                Cancel
              </Button>
              <Button
                type="submit"
                disabled={
                  bookMutation.isPending ||
                  !selectedPatientId ||
                  !selectedDoctorId ||
                  !bookDate ||
                  !bookTime
                }
                className="rounded-xl font-semibold gap-2"
              >
                {bookMutation.isPending ? (
                  <>
                    <RefreshCw className="size-4 animate-spin" /> Booking...
                  </>
                ) : (
                  <>
                    <CalendarDays className="size-4" /> Confirm & Book
                  </>
                )}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Reschedule Modal */}
      <Dialog
        open={Boolean(rescheduleItem)}
        onOpenChange={(open) => !open && setRescheduleItem(null)}
      >
        {rescheduleItem && (
          <DialogContent className="max-w-md rounded-2xl">
            <DialogHeader>
              <DialogTitle className="text-lg font-bold flex items-center gap-2">
                <CalendarClock className="size-5 text-primary" /> Reschedule Appointment
              </DialogTitle>
              <DialogDescription>
                Rescheduling for {rescheduleItem.patientName} with {rescheduleItem.doctorName}.
              </DialogDescription>
            </DialogHeader>

            <div className="space-y-4 pt-2">
              <div className="space-y-1.5">
                <Label htmlFor="reschedDate">New Date *</Label>
                <Input
                  id="reschedDate"
                  type="date"
                  min={todayStr}
                  value={newRescheduleDate}
                  onChange={(e) => setNewRescheduleDate(e.target.value)}
                  className="rounded-xl"
                />
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="reschedTime">New Start Time *</Label>
                <Input
                  id="reschedTime"
                  type="time"
                  value={newRescheduleTime.slice(0, 5)}
                  onChange={(e) => setNewRescheduleTime(`${e.target.value}:00`)}
                  className="rounded-xl"
                />
              </div>
            </div>

            <DialogFooter className="pt-3 flex gap-2 justify-end">
              <Button
                variant="outline"
                className="rounded-xl"
                onClick={() => setRescheduleItem(null)}
              >
                Cancel
              </Button>
              <Button
                className="rounded-xl font-semibold"
                disabled={rescheduleMutation.isPending}
                onClick={() =>
                  rescheduleMutation.mutate({
                    id: rescheduleItem.id,
                    req: {
                      appointmentDate: newRescheduleDate || todayStr,
                      startTime:
                        newRescheduleTime.length === 5
                          ? `${newRescheduleTime}:00`
                          : newRescheduleTime,
                    },
                  })
                }
              >
                {rescheduleMutation.isPending ? "Rescheduling..." : "Save Reschedule"}
              </Button>
            </DialogFooter>
          </DialogContent>
        )}
      </Dialog>

      {/* Cancel Confirmation Dialog */}
      <Dialog open={Boolean(cancelItem)} onOpenChange={(open) => !open && setCancelItem(null)}>
        {cancelItem && (
          <DialogContent className="max-w-md rounded-2xl">
            <DialogHeader>
              <DialogTitle className="text-lg font-bold flex items-center gap-2 text-destructive">
                <Ban className="size-5 text-destructive" /> Cancel Appointment
              </DialogTitle>
              <DialogDescription>
                Are you sure you want to cancel appointment {cancelItem.appointmentNumber} for{" "}
                {cancelItem.patientName}?
              </DialogDescription>
            </DialogHeader>

            <div className="space-y-2 pt-2">
              <Label htmlFor="cancelReason">Reason for Cancellation</Label>
              <Input
                id="cancelReason"
                placeholder="e.g. Patient requested, doctor emergency"
                value={cancelReason}
                onChange={(e) => setCancelReason(e.target.value)}
                className="rounded-xl"
              />
            </div>

            <DialogFooter className="pt-3 flex gap-2 justify-end">
              <Button variant="outline" className="rounded-xl" onClick={() => setCancelItem(null)}>
                Keep Appointment
              </Button>
              <Button
                variant="destructive"
                className="rounded-xl font-semibold"
                disabled={cancelMutation.isPending}
                onClick={() =>
                  cancelMutation.mutate({
                    id: cancelItem.id,
                    reason: cancelReason || "Cancelled by Receptionist",
                  })
                }
              >
                {cancelMutation.isPending ? "Cancelling..." : "Confirm Cancellation"}
              </Button>
            </DialogFooter>
          </DialogContent>
        )}
      </Dialog>
    </div>
  );
}
