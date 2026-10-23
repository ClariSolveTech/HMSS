import { createFileRoute, useSearch, Link } from "@tanstack/react-router";
import { useState, useEffect } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  ListOrdered,
  Footprints,
  RefreshCw,
  AlertCircle,
  PhoneCall,
  RotateCcw,
  FastForward,
  Ban,
  Clock,
  User,
  Stethoscope,
  Filter,
  CheckCircle2,
  Hourglass,
  Ticket,
  Search,
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
  queueApi,
  type QueueEntryResponse,
  type BackendQueueStatus,
  type BackendQueuePriority,
} from "@/services/queue-api";
import { doctorApi, getDoctorFullName } from "@/services/doctor-api";
import { patientApi } from "@/services/patient-api";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/receptionist/queue")({
  validateSearch: (search: Record<string, unknown>): { action?: string | undefined } => ({
    action: typeof search["action"] === "string" ? (search["action"] as string) : undefined,
  }),
  head: () => ({
    meta: [
      { title: "Live OPD Queue — Meridian Health OS" },
      { name: "description", content: "Manage live hospital OPD queue tokens, call, skip, and recall patients." },
    ],
  }),
  component: ReceptionistQueuePage,
});

function ReceptionistQueuePage() {
  const searchParams = useSearch({ from: "/receptionist/queue" });
  const queryClient = useQueryClient();

  const [selectedDoctorId, setSelectedDoctorId] = useState<number | undefined>(undefined);
  const [statusFilter, setStatusFilter] = useState<string>("ALL");
  const [search, setSearch] = useState("");

  // Modals state
  const [walkInOpen, setWalkInOpen] = useState(false);
  const [skipItem, setSkipItem] = useState<QueueEntryResponse | null>(null);
  const [skipReason, setSkipReason] = useState("Patient not in waiting area");
  const [walkInSuccessToken, setWalkInSuccessToken] = useState<QueueEntryResponse | null>(null);

  useEffect(() => {
    if (searchParams.action === "walkin") {
      setWalkInOpen(true);
    }
  }, [searchParams.action]);

  // Fetch doctors for filter & walk-in selection
  const { data: doctorsData } = useQuery({
    queryKey: ["doctors-list"],
    queryFn: () => doctorApi.getDoctors(),
  });
  const doctors = doctorsData?.content || [];

  // Fetch today's queue
  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ["receptionist-today-queue-page", selectedDoctorId],
    queryFn: () => queueApi.getTodayQueue(selectedDoctorId, 0, 100),
    refetchInterval: 10000, // Live poll every 10 seconds for real-time OPD sync
  });

  const queueEntries = data?.content || [];

  // Mutations
  const callMutation = useMutation({
    mutationFn: (id: number) => queueApi.callPatient(id),
    onSuccess: (entry) => {
      toast.success(`Patient #${entry.tokenNumber} called to consultation room!`);
      queryClient.invalidateQueries({ queryKey: ["receptionist-today-queue-page"] });
    },
    onError: (err: any) => {
      toast.error(err.message || "Failed to call patient");
    },
  });

  const skipMutation = useMutation({
    mutationFn: ({ id, reason }: { id: number; reason: string }) =>
      queueApi.skipPatient(id, reason),
    onSuccess: () => {
      toast.success("Patient skipped in queue");
      queryClient.invalidateQueries({ queryKey: ["receptionist-today-queue-page"] });
      setSkipItem(null);
    },
    onError: (err: any) => {
      toast.error(err.message || "Failed to skip patient");
    },
  });

  const recallMutation = useMutation({
    mutationFn: (id: number) => queueApi.recallPatient(id),
    onSuccess: () => {
      toast.success("Patient recalled back to waiting queue!");
      queryClient.invalidateQueries({ queryKey: ["receptionist-today-queue-page"] });
    },
    onError: (err: any) => {
      toast.error(err.message || "Failed to recall patient");
    },
  });

  // Walk-in form state & search
  const [walkInPatientSearch, setWalkInPatientSearch] = useState("");
  const [walkInPatientId, setWalkInPatientId] = useState<number | null>(null);
  const [walkInDoctorId, setWalkInDoctorId] = useState<number | null>(null);
  const [walkInPriority, setWalkInPriority] = useState<BackendQueuePriority>("NORMAL");
  const [walkInNotes, setWalkInNotes] = useState("");

  const { data: walkInPatientResults } = useQuery({
    queryKey: ["walkin-patient-search", walkInPatientSearch],
    queryFn: () => patientApi.getPatients({ search: walkInPatientSearch, size: 5 }),
    enabled: walkInPatientSearch.length >= 2,
  });

  const walkInMutation = useMutation({
    mutationFn: (req: {
      doctorId: number;
      patientId: number;
      priority?: BackendQueuePriority;
      notes?: string;
    }) => queueApi.createWalkIn(req),
    onSuccess: (token) => {
      setWalkInSuccessToken(token);
      toast.success(`Walk-in created! Token #${token.tokenNumber}`);
      queryClient.invalidateQueries({ queryKey: ["receptionist-today-queue-page"] });
      queryClient.invalidateQueries({ queryKey: ["receptionist-today-queue"] });
      resetWalkInForm();
    },
    onError: (err: any) => {
      const msg = err.message || "Failed to create walk-in";
      if (msg.toLowerCase().includes("active") || msg.toLowerCase().includes("duplicate")) {
        toast.error("This patient already has an active queue entry today!");
      } else {
        toast.error(msg);
      }
    },
  });

  const resetWalkInForm = () => {
    setWalkInPatientId(null);
    setWalkInPatientSearch("");
    setWalkInDoctorId(null);
    setWalkInPriority("NORMAL");
    setWalkInNotes("");
  };

  const handleWalkInSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!walkInPatientId || !walkInDoctorId) {
      toast.error("Please select a patient and attending doctor");
      return;
    }
    walkInMutation.mutate({
      doctorId: walkInDoctorId,
      patientId: walkInPatientId,
      priority: walkInPriority,
      notes: walkInNotes || "Walk-In Patient",
    });
  };

  // Filter queue entries
  const filtered = queueEntries.filter((q) => {
    if (statusFilter !== "ALL" && q.status !== statusFilter) return false;
    if (!search.trim()) return true;
    const term = search.toLowerCase();
    return (
      q.patientName?.toLowerCase().includes(term) ||
      q.doctorName?.toLowerCase().includes(term) ||
      String(q.tokenNumber).includes(term) ||
      q.patientCode?.toLowerCase().includes(term)
    );
  });

  // Calculate summary counts
  const waitingCount = queueEntries.filter((q) => q.status === "WAITING").length;
  const calledCount = queueEntries.filter((q) => q.status === "CALLED" || q.status === "IN_CONSULTATION").length;
  const completedCount = queueEntries.filter((q) => q.status === "COMPLETED").length;
  const skippedCount = queueEntries.filter((q) => q.status === "SKIPPED").length;

  return (
    <div className="space-y-6 p-4 sm:p-6 lg:p-8">
      <PageHeader
        title="Live OPD Queue"
        subtitle="Real-time patient queue management, token announcements, call, and walk-in issuance."
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
            <Button
              size="sm"
              className="rounded-xl gap-2"
              onClick={() => setWalkInOpen(true)}
            >
              <Footprints className="size-4" /> Issue Walk-In
            </Button>
          </div>
        }
      />

      {/* Queue Stat Summary */}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <div className="surface-card rounded-2xl p-4 border shadow-sm">
          <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wider block">
            Waiting in OPD
          </span>
          <div className="text-2xl font-black text-amber-600 mt-1">{waitingCount}</div>
        </div>

        <div className="surface-card rounded-2xl p-4 border shadow-sm">
          <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wider block">
            In Consultation / Called
          </span>
          <div className="text-2xl font-black text-blue-600 mt-1">{calledCount}</div>
        </div>

        <div className="surface-card rounded-2xl p-4 border shadow-sm">
          <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wider block">
            Completed Visits
          </span>
          <div className="text-2xl font-black text-emerald-600 mt-1">{completedCount}</div>
        </div>

        <div className="surface-card rounded-2xl p-4 border shadow-sm">
          <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wider block">
            Skipped / Absent
          </span>
          <div className="text-2xl font-black text-muted-foreground mt-1">{skippedCount}</div>
        </div>
      </div>

      {/* Filter Strip */}
      <div className="surface-card rounded-2xl p-4 border shadow-sm flex flex-col md:flex-row gap-3 items-center justify-between">
        <div className="flex flex-wrap items-center gap-3 w-full md:w-auto">
          {/* Doctor Filter */}
          <div className="flex items-center gap-2">
            <Label htmlFor="doctor-filter" className="text-xs text-muted-foreground whitespace-nowrap">
              Doctor:
            </Label>
            <select
              id="doctor-filter"
              value={selectedDoctorId || ""}
              onChange={(e) =>
                setSelectedDoctorId(e.target.value ? Number(e.target.value) : undefined)
              }
              className="h-8 rounded-xl border bg-background px-2.5 text-xs outline-none focus:border-primary max-w-xs"
            >
              <option value="">All Doctors</option>
              {doctors.map((doc) => (
                <option key={doc.id} value={doc.id}>
                  Dr. {getDoctorFullName(doc)}
                </option>
              ))}
            </select>
          </div>

          {/* Status Filter */}
          <div className="flex items-center gap-2">
            <Label htmlFor="status-queue-filter" className="text-xs text-muted-foreground whitespace-nowrap">
              Status:
            </Label>
            <select
              id="status-queue-filter"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="h-8 rounded-xl border bg-background px-2.5 text-xs outline-none focus:border-primary"
            >
              <option value="ALL">All Entries</option>
              <option value="WAITING">Waiting</option>
              <option value="CALLED">Called</option>
              <option value="IN_CONSULTATION">In Consultation</option>
              <option value="SKIPPED">Skipped</option>
              <option value="COMPLETED">Completed</option>
            </select>
          </div>
        </div>

        {/* Search Input */}
        <div className="relative w-full md:w-64">
          <Search className="absolute left-3 top-2.5 size-4 text-muted-foreground" />
          <Input
            placeholder="Search token, patient, code..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="pl-9 h-8 rounded-xl text-xs"
          />
        </div>
      </div>

      {/* Queue Token List */}
      {isLoading ? (
        <div className="surface-card rounded-2xl p-16 text-center border">
          <RefreshCw className="size-8 animate-spin mx-auto text-primary mb-3" />
          <p className="text-sm font-semibold">Loading live queue...</p>
        </div>
      ) : isError ? (
        <div className="surface-card rounded-2xl p-12 text-center border border-destructive/20 bg-destructive/5">
          <AlertCircle className="size-8 text-destructive mx-auto mb-3" />
          <p className="text-sm font-semibold text-destructive">Failed to load queue</p>
          <Button variant="outline" size="sm" className="mt-3 rounded-xl" onClick={() => refetch()}>
            Retry
          </Button>
        </div>
      ) : filtered.length === 0 ? (
        <div className="surface-card rounded-2xl p-16 text-center border">
          <ListOrdered className="size-10 text-muted-foreground/30 mx-auto mb-3" />
          <h3 className="text-base font-bold">No queue entries found</h3>
          <p className="text-xs text-muted-foreground mt-1 max-w-sm mx-auto">
            {selectedDoctorId || statusFilter !== "ALL"
              ? "No tokens match the active filters."
              : "No patients are currently queued for today. Check in an appointment or create a walk-in token."}
          </p>
          <div className="flex justify-center gap-2 mt-4">
            <Link to="/receptionist/check-in">
              <Button size="sm" className="rounded-xl gap-2">
                <CheckCircle2 className="size-4" /> Check In Patient
              </Button>
            </Link>
            <Button
              size="sm"
              variant="outline"
              className="rounded-xl gap-2"
              onClick={() => setWalkInOpen(true)}
            >
              <Footprints className="size-4" /> Issue Walk-In
            </Button>
          </div>
        </div>
      ) : (
        <div className="surface-card rounded-2xl border overflow-hidden shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="border-b bg-muted/40 font-semibold text-muted-foreground">
                <tr>
                  <th className="p-3.5">Token</th>
                  <th className="p-3.5">Patient Details</th>
                  <th className="p-3.5">Doctor</th>
                  <th className="p-3.5">Check-In Time</th>
                  <th className="p-3.5">Priority</th>
                  <th className="p-3.5">Status</th>
                  <th className="p-3.5 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {filtered.map((q) => (
                  <tr key={q.id} className="hover:bg-muted/30 transition-colors">
                    <td className="p-3.5 font-mono font-black text-base text-primary">
                      <span className="flex size-9 items-center justify-center rounded-xl bg-primary text-primary-foreground font-black text-sm shadow-sm">
                        #{q.tokenNumber}
                      </span>
                    </td>
                    <td className="p-3.5">
                      <p className="font-bold text-foreground text-sm">
                        {q.patientName || `Patient #${q.patientId}`}
                      </p>
                      <p className="text-[11px] text-muted-foreground font-mono">
                        {q.patientCode || `ID: #${q.patientId}`}
                      </p>
                    </td>
                    <td className="p-3.5">
                      <p className="font-medium text-foreground">
                        {q.doctorName ? `Dr. ${q.doctorName}` : `Doctor #${q.doctorId}`}
                      </p>
                    </td>
                    <td className="p-3.5 text-muted-foreground">
                      <span className="flex items-center gap-1.5">
                        <Clock className="size-3.5 text-muted-foreground" />
                        {q.checkedInAt ? new Date(q.checkedInAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }) : "—"}
                      </span>
                    </td>
                    <td className="p-3.5">
                      <Badge
                        variant="outline"
                        className={cn(
                          "text-[10px] rounded-full font-semibold",
                          q.priority === "EMERGENCY" && "bg-destructive/10 text-destructive border-destructive/30",
                          q.priority === "URGENT" && "bg-amber-500/10 text-amber-600 border-amber-300",
                          q.priority === "NORMAL" && "bg-muted text-muted-foreground",
                        )}
                      >
                        {q.priority}
                      </Badge>
                    </td>
                    <td className="p-3.5">
                      <Badge
                        className={cn(
                          "text-[10px] font-semibold rounded-full border-0",
                          q.status === "WAITING" && "bg-amber-500/10 text-amber-600",
                          q.status === "CALLED" && "bg-blue-500/10 text-blue-600",
                          q.status === "IN_CONSULTATION" && "bg-purple-500/10 text-purple-600",
                          q.status === "COMPLETED" && "bg-emerald-500/10 text-emerald-600",
                          q.status === "SKIPPED" && "bg-muted text-muted-foreground",
                          q.status === "CANCELLED" && "bg-destructive/10 text-destructive",
                        )}
                      >
                        {q.status}
                      </Badge>
                    </td>
                    <td className="p-3.5 text-right whitespace-nowrap">
                      <div className="flex items-center justify-end gap-1.5">
                        {/* Call Patient */}
                        {q.status === "WAITING" && (
                          <Button
                            size="sm"
                            className="rounded-lg h-7 text-xs gap-1 bg-blue-600 hover:bg-blue-700 text-white"
                            onClick={() => callMutation.mutate(q.id)}
                            disabled={callMutation.isPending}
                          >
                            <PhoneCall className="size-3" /> Call
                          </Button>
                        )}

                        {/* Skip Patient */}
                        {(q.status === "WAITING" || q.status === "CALLED") && (
                          <Button
                            size="sm"
                            variant="outline"
                            className="rounded-lg h-7 text-xs text-amber-700 border-amber-300 hover:bg-amber-50"
                            onClick={() => setSkipItem(q)}
                          >
                            <FastForward className="size-3" /> Skip
                          </Button>
                        )}

                        {/* Recall Patient */}
                        {q.status === "SKIPPED" && (
                          <Button
                            size="sm"
                            className="rounded-lg h-7 text-xs gap-1 bg-purple-600 hover:bg-purple-700 text-white"
                            onClick={() => recallMutation.mutate(q.id)}
                            disabled={recallMutation.isPending}
                          >
                            <RotateCcw className="size-3" /> Recall
                          </Button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Skip Patient Confirmation Dialog */}
      <Dialog open={Boolean(skipItem)} onOpenChange={(open) => !open && setSkipItem(null)}>
        {skipItem && (
          <DialogContent className="max-w-md rounded-2xl">
            <DialogHeader>
              <DialogTitle className="text-lg font-bold flex items-center gap-2">
                <FastForward className="size-5 text-amber-600" /> Skip Token #{skipItem.tokenNumber}
              </DialogTitle>
              <DialogDescription>
                Patient {skipItem.patientName} will be moved to SKIPPED status. You can recall them later if they arrive.
              </DialogDescription>
            </DialogHeader>

            <div className="space-y-2 pt-2">
              <Label htmlFor="skipReason">Reason</Label>
              <Input
                id="skipReason"
                value={skipReason}
                onChange={(e) => setSkipReason(e.target.value)}
                className="rounded-xl"
              />
            </div>

            <DialogFooter className="pt-3 flex gap-2 justify-end">
              <Button
                variant="outline"
                className="rounded-xl"
                onClick={() => setSkipItem(null)}
              >
                Cancel
              </Button>
              <Button
                className="rounded-xl font-semibold bg-amber-600 hover:bg-amber-700 text-white"
                disabled={skipMutation.isPending}
                onClick={() =>
                  skipMutation.mutate({
                    id: skipItem.id,
                    reason: skipReason || "Patient not present",
                  })
                }
              >
                {skipMutation.isPending ? "Skipping..." : "Confirm Skip"}
              </Button>
            </DialogFooter>
          </DialogContent>
        )}
      </Dialog>

      {/* Walk-In Creation Modal */}
      <Dialog open={walkInOpen} onOpenChange={setWalkInOpen}>
        <DialogContent className="max-w-xl max-h-[90vh] overflow-y-auto rounded-2xl">
          <DialogHeader>
            <DialogTitle className="text-xl font-bold flex items-center gap-2">
              <Footprints className="size-5 text-amber-600" /> Issue Walk-In Token
            </DialogTitle>
            <DialogDescription>
              Assign immediate queue token for patient arriving without a prior appointment.
            </DialogDescription>
          </DialogHeader>

          <form onSubmit={handleWalkInSubmit} className="space-y-4 pt-2">
            {/* Patient Search & Select */}
            <div className="space-y-1.5">
              <Label>Select Patient *</Label>
              {walkInPatientId ? (
                <div className="surface-card rounded-xl p-3 border flex items-center justify-between">
                  <div className="flex items-center gap-2.5">
                    <User className="size-4 text-primary" />
                    <div>
                      <p className="text-xs font-bold text-foreground">
                        Selected Patient ID: #{walkInPatientId}
                      </p>
                      <p className="text-[11px] text-muted-foreground">{walkInPatientSearch}</p>
                    </div>
                  </div>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    className="h-7 text-xs"
                    onClick={() => {
                      setWalkInPatientId(null);
                      setWalkInPatientSearch("");
                    }}
                  >
                    Change
                  </Button>
                </div>
              ) : (
                <div className="space-y-2">
                  <Input
                    placeholder="Search existing patient by name or phone..."
                    value={walkInPatientSearch}
                    onChange={(e) => setWalkInPatientSearch(e.target.value)}
                    className="rounded-xl text-sm"
                  />
                  {walkInPatientResults && walkInPatientResults.content.length > 0 && (
                    <div className="rounded-xl border bg-popover p-1 shadow-md space-y-1">
                      {walkInPatientResults.content.map((p) => (
                        <button
                          key={p.id}
                          type="button"
                          className="w-full text-left p-2 rounded-lg hover:bg-muted flex items-center justify-between text-xs"
                          onClick={() => {
                            setWalkInPatientId(p.id);
                            setWalkInPatientSearch(`${p.firstName} ${p.lastName} (${p.patientCode})`);
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
                    New patient?{" "}
                    <Link to="/receptionist/patients" search={{ action: "register" }} className="text-primary font-semibold hover:underline">
                      Register first
                    </Link>
                  </p>
                </div>
              )}
            </div>

            {/* Doctor Select */}
            <div className="space-y-1.5">
              <Label htmlFor="walkInDoc">Doctor *</Label>
              <select
                id="walkInDoc"
                required
                value={walkInDoctorId || ""}
                onChange={(e) => setWalkInDoctorId(Number(e.target.value))}
                className="w-full rounded-xl border bg-background px-3 py-2 text-sm outline-none focus:border-primary"
              >
                <option value="">Select Doctor</option>
                {doctors.map((doc) => (
                  <option key={doc.id} value={doc.id}>
                    Dr. {getDoctorFullName(doc)} ({doc.specialization || "General"})
                  </option>
                ))}
              </select>
            </div>

            {/* Priority */}
            <div className="space-y-1.5">
              <Label htmlFor="walkInPriority">Priority</Label>
              <select
                id="walkInPriority"
                value={walkInPriority}
                onChange={(e) => setWalkInPriority(e.target.value as BackendQueuePriority)}
                className="w-full rounded-xl border bg-background px-3 py-2 text-sm outline-none focus:border-primary"
              >
                <option value="NORMAL">Normal</option>
                <option value="URGENT">Urgent</option>
                <option value="EMERGENCY">Emergency</option>
              </select>
            </div>

            {/* Notes */}
            <div className="space-y-1.5">
              <Label htmlFor="walkInNotes">Notes / Symptom</Label>
              <Input
                id="walkInNotes"
                placeholder="e.g. Acute pain, urgent walk-in"
                value={walkInNotes}
                onChange={(e) => setWalkInNotes(e.target.value)}
                className="rounded-xl"
              />
            </div>

            <DialogFooter className="pt-3 flex gap-2 justify-end">
              <Button
                type="button"
                variant="outline"
                className="rounded-xl"
                onClick={() => setWalkInOpen(false)}
              >
                Cancel
              </Button>
              <Button
                type="submit"
                disabled={walkInMutation.isPending}
                className="rounded-xl font-semibold gap-2 bg-amber-600 hover:bg-amber-700 text-white"
              >
                {walkInMutation.isPending ? (
                  <>
                    <RefreshCw className="size-4 animate-spin" /> Issuing...
                  </>
                ) : (
                  <>
                    <Ticket className="size-4" /> Issue Token
                  </>
                )}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Walk-in Success Token Confirmation Dialog */}
      <Dialog
        open={Boolean(walkInSuccessToken)}
        onOpenChange={(open) => !open && setWalkInSuccessToken(null)}
      >
        {walkInSuccessToken && (
          <DialogContent className="max-w-sm text-center rounded-2xl">
            <DialogHeader>
              <div className="size-16 rounded-full bg-emerald-500 text-white grid place-items-center mx-auto mb-2 shadow-md">
                <Ticket className="size-8" />
              </div>
              <DialogTitle className="text-xl font-black">Walk-In Token Issued</DialogTitle>
              <DialogDescription>
                Patient has been added to the doctor's live queue.
              </DialogDescription>
            </DialogHeader>

            <div className="surface-card rounded-2xl p-5 border bg-background my-2">
              <span className="text-xs text-muted-foreground uppercase font-semibold">
                Token Number
              </span>
              <div className="text-5xl font-black text-primary my-1">
                #{walkInSuccessToken.tokenNumber}
              </div>
              <p className="text-xs font-semibold text-foreground mt-2">
                {walkInSuccessToken.patientName}
              </p>
              <p className="text-[11px] text-muted-foreground">
                Dr. {walkInSuccessToken.doctorName} · {walkInSuccessToken.priority}
              </p>
            </div>

            <DialogFooter>
              <Button
                className="w-full rounded-xl"
                onClick={() => setWalkInSuccessToken(null)}
              >
                Done
              </Button>
            </DialogFooter>
          </DialogContent>
        )}
      </Dialog>
    </div>
  );
}
