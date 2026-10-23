import { createFileRoute, useSearch } from "@tanstack/react-router";
import { useState, useEffect } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  Users,
  Search,
  UserPlus,
  RefreshCw,
  AlertCircle,
  Phone,
  Mail,
  Calendar,
  MapPin,
  Heart,
  UserCheck,
  ChevronLeft,
  ChevronRight,
  X,
  FileText,
  KeyRound,
  Copy,
  Check,
  ShieldCheck,
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
  patientApi,
  type PatientResponse,
  type CreatePatientRequest,
  type PatientActivationCodeResponse,
  type Gender,
  type BloodGroup,
} from "@/services/patient-api";

export const Route = createFileRoute("/receptionist/patients")({
  validateSearch: (search: Record<string, unknown>): { action?: string | undefined } => ({
    action: search["action"] as string | undefined,
  }),
  head: () => ({
    meta: [
      { title: "Patients Directory — Meridian Health OS" },
      { name: "description", content: "Register, search, and manage hospital patients." },
    ],
  }),
  component: ReceptionistPatientsPage,
});

function ReceptionistPatientsPage() {
  const searchParams = useSearch({ from: "/receptionist/patients" });
  const queryClient = useQueryClient();

  const [search, setSearch] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [page, setPage] = useState(0);
  const pageSize = 15;

  const [registerOpen, setRegisterOpen] = useState(false);
  const [selectedPatient, setSelectedPatient] = useState<PatientResponse | null>(null);

  // Activation code state
  const [activationModalOpen, setActivationModalOpen] = useState(false);
  const [activationData, setActivationData] = useState<PatientActivationCodeResponse | null>(null);
  const [codeCopied, setCodeCopied] = useState(false);

  const generateActivationMutation = useMutation({
    mutationFn: (patientId: number) => patientApi.generateActivationCode(patientId),
    onSuccess: (resData) => {
      setActivationData(resData);
      setActivationModalOpen(true);
      toast.success("Activation code generated!");
      queryClient.invalidateQueries({ queryKey: ["patients-list"] });
    },
    onError: (err: unknown) => {
      const msg = err instanceof Error ? err.message : "Failed to generate activation code";
      toast.error(msg);
    },
  });

  // Sync action=register query parameter to open registration modal automatically
  useEffect(() => {
    if (searchParams.action === "register") {
      setRegisterOpen(true);
    }
  }, [searchParams.action]);

  // Debounce search input
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedSearch(search);
      setPage(0);
    }, 300);
    return () => clearTimeout(handler);
  }, [search]);

  // Fetch patients from backend
  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ["patients-list", debouncedSearch, page],
    queryFn: () =>
      patientApi.getPatients({
        search: debouncedSearch.trim() || undefined,
        page,
        size: pageSize,
      }),
  });

  // Patient registration mutation
  const [form, setForm] = useState<CreatePatientRequest>({
    firstName: "",
    lastName: "",
    dateOfBirth: "",
    gender: "MALE",
    mobileNumber: "",
    email: "",
    bloodGroup: undefined,
    address: "",
    city: "",
    state: "",
    pincode: "",
    emergencyContactName: "",
    emergencyContactNumber: "",
  });

  const createMutation = useMutation({
    mutationFn: (req: CreatePatientRequest) => patientApi.createPatient(req),
    onSuccess: (newPatient) => {
      toast.success(`Patient registered! Code: ${newPatient.patientCode}`);
      queryClient.invalidateQueries({ queryKey: ["patients-list"] });
      queryClient.invalidateQueries({ queryKey: ["patients-directory"] });
      queryClient.invalidateQueries({ queryKey: ["doctor-patient-search"] });
      queryClient.invalidateQueries({ queryKey: ["patient-search-booking"] });
      queryClient.invalidateQueries({ queryKey: ["walkin-patient-search"] });
      setRegisterOpen(false);
      resetForm();
    },
    onError: (err: unknown) => {
      const msg = err instanceof Error ? err.message : "Failed to register patient";
      toast.error(msg);
    },
  });

  const resetForm = () => {
    setForm({
      firstName: "",
      lastName: "",
      dateOfBirth: "",
      gender: "MALE",
      mobileNumber: "",
      email: "",
      bloodGroup: undefined,
      address: "",
      city: "",
      state: "",
      pincode: "",
      emergencyContactName: "",
      emergencyContactNumber: "",
    });
  };

  const handleRegisterSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.firstName || !form.lastName || !form.dateOfBirth || !form.mobileNumber) {
      toast.error("Please fill all required fields (*)");
      return;
    }
    createMutation.mutate(form);
  };

  const patients = data?.content || [];
  const totalPages = data?.totalPages || 0;
  const totalElements = data?.totalElements || 0;

  return (
    <div className="space-y-6 p-4 sm:p-6 lg:p-8">
      <PageHeader
        title="Patients Directory"
        subtitle="Universal patient registry with name, mobile, and patient code search."
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
            <Button size="sm" className="rounded-xl gap-2" onClick={() => setRegisterOpen(true)}>
              <UserPlus className="size-4" /> Register Patient
            </Button>
          </div>
        }
      />

      {/* Search Bar */}
      <div className="surface-card rounded-2xl p-4 border shadow-sm flex flex-col sm:flex-row gap-3 items-center justify-between">
        <div className="relative w-full sm:max-w-md">
          <Search className="absolute left-3 top-3 size-4 text-muted-foreground" />
          <Input
            placeholder="Search by patient name, mobile number, or code..."
            className="pl-9 rounded-xl text-sm"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          {search && (
            <button
              onClick={() => setSearch("")}
              className="absolute right-3 top-3 text-muted-foreground hover:text-foreground"
            >
              <X className="size-4" />
            </button>
          )}
        </div>
        <p className="text-xs text-muted-foreground shrink-0">
          Showing {patients.length} of {totalElements} patient records
        </p>
      </div>

      {/* Patient List Table */}
      {isLoading ? (
        <div className="surface-card rounded-2xl p-16 text-center border">
          <RefreshCw className="size-8 animate-spin mx-auto text-primary mb-3" />
          <p className="text-sm font-semibold">Searching patient registry...</p>
        </div>
      ) : isError ? (
        <div className="surface-card rounded-2xl p-12 text-center border border-destructive/20 bg-destructive/5">
          <AlertCircle className="size-8 text-destructive mx-auto mb-3" />
          <p className="text-sm font-semibold text-destructive">Failed to fetch patients</p>
          <Button variant="outline" size="sm" className="mt-3 rounded-xl" onClick={() => refetch()}>
            Retry
          </Button>
        </div>
      ) : patients.length === 0 ? (
        <div className="surface-card rounded-2xl p-16 text-center border">
          <Users className="size-10 text-muted-foreground/30 mx-auto mb-3" />
          <h3 className="text-base font-bold">No patients found</h3>
          <p className="text-xs text-muted-foreground mt-1 max-w-sm mx-auto">
            {debouncedSearch
              ? `No patient records match "${debouncedSearch}". Try a different name or mobile.`
              : "No patients registered in this hospital yet. Start by registering the first patient."}
          </p>
          <Button size="sm" className="mt-4 rounded-xl gap-2" onClick={() => setRegisterOpen(true)}>
            <UserPlus className="size-4" /> Register Patient Now
          </Button>
        </div>
      ) : (
        <div className="surface-card rounded-2xl border overflow-hidden shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="border-b bg-muted/40 font-semibold text-muted-foreground">
                <tr>
                  <th className="p-3.5">Code</th>
                  <th className="p-3.5">Patient Name</th>
                  <th className="p-3.5">Gender / DOB</th>
                  <th className="p-3.5">Contact</th>
                  <th className="p-3.5">Blood Group</th>
                  <th className="p-3.5">Location</th>
                  <th className="p-3.5 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {patients.map((p) => (
                  <tr
                    key={p.id}
                    className="hover:bg-muted/30 transition-colors cursor-pointer"
                    onClick={() => setSelectedPatient(p)}
                  >
                    <td className="p-3.5 font-mono font-semibold text-primary">{p.patientCode}</td>
                    <td className="p-3.5">
                      <p className="font-bold text-foreground text-sm">
                        {p.firstName} {p.lastName}
                      </p>
                      <p className="text-[11px] text-muted-foreground">{p.email || "No email"}</p>
                    </td>
                    <td className="p-3.5 text-muted-foreground">
                      <span className="font-medium text-foreground">{p.gender}</span> ·{" "}
                      {p.dateOfBirth}
                    </td>
                    <td className="p-3.5 font-mono">{p.mobileNumber}</td>
                    <td className="p-3.5">
                      {p.bloodGroup ? (
                        <Badge
                          variant="outline"
                          className="text-[10px] font-bold text-destructive bg-destructive/5 border-destructive/20 rounded-full"
                        >
                          {p.bloodGroup.replace("_", " ")}
                        </Badge>
                      ) : (
                        <span className="text-muted-foreground">—</span>
                      )}
                    </td>
                    <td className="p-3.5 text-muted-foreground">
                      {p.city ? `${p.city}, ${p.state || ""}` : "—"}
                    </td>
                    <td className="p-3.5 text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        {!p.userId ? (
                          <Button
                            size="sm"
                            variant="outline"
                            className="rounded-lg h-7 text-[11px] px-2.5 font-bold border-indigo-200 text-indigo-700 hover:bg-indigo-50"
                            onClick={(e) => {
                              e.stopPropagation();
                              generateActivationMutation.mutate(p.id);
                            }}
                            disabled={generateActivationMutation.isPending}
                          >
                            <KeyRound className="size-3 mr-1" /> Activate
                          </Button>
                        ) : (
                          <Badge
                            variant="outline"
                            className="text-[10px] bg-emerald-50 text-emerald-700 border-emerald-200 rounded-full font-bold"
                          >
                            Portal Active
                          </Badge>
                        )}
                        <Button
                          size="sm"
                          variant="ghost"
                          className="rounded-lg h-7 text-xs text-primary font-semibold"
                          onClick={(e) => {
                            e.stopPropagation();
                            setSelectedPatient(p);
                          }}
                        >
                          View File
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination Controls */}
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

      {/* Patient Registration Dialog */}
      <Dialog open={registerOpen} onOpenChange={setRegisterOpen}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto rounded-2xl">
          <DialogHeader>
            <DialogTitle className="text-xl font-bold flex items-center gap-2">
              <UserPlus className="size-5 text-primary" /> Register New Patient
            </DialogTitle>
            <DialogDescription>
              Hospital ID is automatically secured by session. Enter patient identification details
              below.
            </DialogDescription>
          </DialogHeader>

          <form onSubmit={handleRegisterSubmit} className="space-y-4 pt-2">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label htmlFor="firstName">First Name *</Label>
                <Input
                  id="firstName"
                  required
                  placeholder="e.g. Ramesh"
                  value={form.firstName}
                  onChange={(e) => setForm({ ...form, firstName: e.target.value })}
                  className="rounded-xl"
                />
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="lastName">Last Name *</Label>
                <Input
                  id="lastName"
                  required
                  placeholder="e.g. Kumar"
                  value={form.lastName}
                  onChange={(e) => setForm({ ...form, lastName: e.target.value })}
                  className="rounded-xl"
                />
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="dateOfBirth">Date of Birth *</Label>
                <Input
                  id="dateOfBirth"
                  type="date"
                  required
                  value={form.dateOfBirth}
                  onChange={(e) => setForm({ ...form, dateOfBirth: e.target.value })}
                  className="rounded-xl"
                />
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="gender">Gender *</Label>
                <select
                  id="gender"
                  value={form.gender}
                  onChange={(e) => setForm({ ...form, gender: e.target.value as Gender })}
                  className="w-full rounded-xl border bg-background px-3 py-2 text-sm outline-none focus:border-primary"
                >
                  <option value="MALE">Male</option>
                  <option value="FEMALE">Female</option>
                  <option value="OTHER">Other</option>
                </select>
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="mobileNumber">Mobile Number *</Label>
                <Input
                  id="mobileNumber"
                  type="tel"
                  required
                  placeholder="10-digit mobile (e.g. 9876543210)"
                  value={form.mobileNumber}
                  onChange={(e) => setForm({ ...form, mobileNumber: e.target.value })}
                  className="rounded-xl"
                />
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="email">Email (Optional)</Label>
                <Input
                  id="email"
                  type="email"
                  placeholder="patient@example.com"
                  value={form.email || ""}
                  onChange={(e) => setForm({ ...form, email: e.target.value })}
                  className="rounded-xl"
                />
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="bloodGroup">Blood Group</Label>
                <select
                  id="bloodGroup"
                  value={form.bloodGroup || ""}
                  onChange={(e) =>
                    setForm({
                      ...form,
                      bloodGroup: e.target.value ? (e.target.value as BloodGroup) : undefined,
                    })
                  }
                  className="w-full rounded-xl border bg-background px-3 py-2 text-sm outline-none focus:border-primary"
                >
                  <option value="">Unknown / Select</option>
                  <option value="A_POSITIVE">A+</option>
                  <option value="A_NEGATIVE">A-</option>
                  <option value="B_POSITIVE">B+</option>
                  <option value="B_NEGATIVE">B-</option>
                  <option value="AB_POSITIVE">AB+</option>
                  <option value="AB_NEGATIVE">AB-</option>
                  <option value="O_POSITIVE">O+</option>
                  <option value="O_NEGATIVE">O-</option>
                </select>
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="city">City</Label>
                <Input
                  id="city"
                  placeholder="e.g. Bengaluru"
                  value={form.city || ""}
                  onChange={(e) => setForm({ ...form, city: e.target.value })}
                  className="rounded-xl"
                />
              </div>
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="address">Residential Address</Label>
              <Input
                id="address"
                placeholder="Street address, apartment, locality"
                value={form.address || ""}
                onChange={(e) => setForm({ ...form, address: e.target.value })}
                className="rounded-xl"
              />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label htmlFor="emergencyContactName">Emergency Contact Person</Label>
                <Input
                  id="emergencyContactName"
                  placeholder="Name of relative/guardian"
                  value={form.emergencyContactName || ""}
                  onChange={(e) => setForm({ ...form, emergencyContactName: e.target.value })}
                  className="rounded-xl"
                />
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="emergencyContactNumber">Emergency Contact Phone</Label>
                <Input
                  id="emergencyContactNumber"
                  placeholder="Emergency phone number"
                  value={form.emergencyContactNumber || ""}
                  onChange={(e) => setForm({ ...form, emergencyContactNumber: e.target.value })}
                  className="rounded-xl"
                />
              </div>
            </div>

            <DialogFooter className="pt-4 flex gap-2 justify-end">
              <Button
                type="button"
                variant="outline"
                className="rounded-xl"
                onClick={() => setRegisterOpen(false)}
              >
                Cancel
              </Button>
              <Button
                type="submit"
                disabled={createMutation.isPending}
                className="rounded-xl font-semibold gap-2"
              >
                {createMutation.isPending ? (
                  <>
                    <RefreshCw className="size-4 animate-spin" /> Saving...
                  </>
                ) : (
                  <>
                    <UserCheck className="size-4" /> Save Patient
                  </>
                )}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Patient Detail Modal */}
      <Dialog
        open={Boolean(selectedPatient)}
        onOpenChange={(open) => !open && setSelectedPatient(null)}
      >
        {selectedPatient && (
          <DialogContent className="max-w-md rounded-2xl">
            <DialogHeader>
              <div className="flex items-center gap-3">
                <div className="size-12 rounded-2xl bg-primary/10 text-primary grid place-items-center font-bold text-lg">
                  {selectedPatient.firstName[0]}
                  {selectedPatient.lastName[0]}
                </div>
                <div>
                  <DialogTitle className="text-lg font-bold">
                    {selectedPatient.firstName} {selectedPatient.lastName}
                  </DialogTitle>
                  <p className="text-xs font-mono text-primary">{selectedPatient.patientCode}</p>
                </div>
              </div>
            </DialogHeader>

            <div className="space-y-3 pt-2 text-xs">
              <div className="grid grid-cols-2 gap-3 surface-card rounded-xl p-3 border">
                <div>
                  <p className="text-muted-foreground">Date of Birth</p>
                  <p className="font-semibold text-foreground">{selectedPatient.dateOfBirth}</p>
                </div>
                <div>
                  <p className="text-muted-foreground">Gender</p>
                  <p className="font-semibold text-foreground">{selectedPatient.gender}</p>
                </div>
                <div>
                  <p className="text-muted-foreground">Mobile</p>
                  <p className="font-semibold text-foreground">{selectedPatient.mobileNumber}</p>
                </div>
                <div>
                  <p className="text-muted-foreground">Blood Group</p>
                  <p className="font-semibold text-foreground">
                    {selectedPatient.bloodGroup?.replace("_", " ") || "Not recorded"}
                  </p>
                </div>
              </div>

              {selectedPatient.email && (
                <div className="flex items-center gap-2 text-muted-foreground">
                  <Mail className="size-3.5 shrink-0" />
                  <span className="text-foreground">{selectedPatient.email}</span>
                </div>
              )}

              {selectedPatient.address && (
                <div className="flex items-start gap-2 text-muted-foreground">
                  <MapPin className="size-3.5 shrink-0 mt-0.5" />
                  <span className="text-foreground">
                    {selectedPatient.address}, {selectedPatient.city || ""}{" "}
                    {selectedPatient.pincode || ""}
                  </span>
                </div>
              )}

              {selectedPatient.emergencyContactName && (
                <div className="surface-card rounded-xl p-3 border space-y-1">
                  <p className="text-[11px] font-semibold text-muted-foreground uppercase">
                    Emergency Contact
                  </p>
                  <p className="font-semibold text-foreground">
                    {selectedPatient.emergencyContactName} (
                    {selectedPatient.emergencyContactNumber || "No number"})
                  </p>
                </div>
              )}

              {/* Portal Access Status & Activation Action */}
              <div className="surface-card rounded-xl p-3 border space-y-2">
                <div className="flex items-center justify-between">
                  <span className="font-semibold text-foreground">Patient Portal Access</span>
                  {selectedPatient.userId ? (
                    <Badge className="bg-emerald-100 text-emerald-800 border-0 rounded-full text-[11px] font-bold">
                      Account Active
                    </Badge>
                  ) : (
                    <Badge className="bg-amber-100 text-amber-800 border-0 rounded-full text-[11px] font-bold">
                      No Portal Account
                    </Badge>
                  )}
                </div>
                {!selectedPatient.userId ? (
                  <div className="space-y-2 pt-1">
                    <p className="text-[11px] text-muted-foreground leading-relaxed">
                      This patient does not have a login account yet. Generate an activation code so
                      the patient can establish their password and access their appointments and
                      teleconsultations.
                    </p>
                    <Button
                      type="button"
                      size="sm"
                      disabled={generateActivationMutation.isPending}
                      onClick={() => generateActivationMutation.mutate(selectedPatient.id)}
                      className="h-8 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs gap-1.5"
                    >
                      <KeyRound className="size-3.5" />
                      {generateActivationMutation.isPending
                        ? "Generating..."
                        : "Generate Portal Activation Code"}
                    </Button>
                  </div>
                ) : (
                  <p className="text-[11px] text-muted-foreground">
                    User ID #{selectedPatient.userId} is linked to this clinical record.
                  </p>
                )}
              </div>
            </div>

            <DialogFooter className="pt-2">
              <Button
                variant="outline"
                className="w-full rounded-xl"
                onClick={() => setSelectedPatient(null)}
              >
                Close
              </Button>
            </DialogFooter>
          </DialogContent>
        )}
      </Dialog>

      {/* Activation Code Dialog */}
      <Dialog open={activationModalOpen} onOpenChange={setActivationModalOpen}>
        <DialogContent className="max-w-md rounded-2xl">
          <DialogHeader>
            <DialogTitle className="text-lg font-bold flex items-center gap-2 text-indigo-950">
              <KeyRound className="size-5 text-indigo-600" /> Patient Portal Activation Code
            </DialogTitle>
            <DialogDescription>
              One-time expiring access code for {activationData?.patientName} (
              {activationData?.patientCode})
            </DialogDescription>
          </DialogHeader>

          {activationData && (
            <div className="space-y-4 pt-2">
              <div className="rounded-2xl border-2 border-indigo-200 bg-indigo-50/70 p-4 text-center space-y-2">
                <span className="text-[11px] font-bold text-indigo-700 uppercase tracking-wider">
                  One-Time Activation Code
                </span>
                <div className="font-mono text-2xl font-black text-indigo-950 tracking-widest select-all">
                  {activationData.activationCode}
                </div>
                <p className="text-[11px] text-indigo-700">
                  Expires: {new Date(activationData.expiresAt).toLocaleString()} (24 hours)
                </p>
              </div>

              <div className="space-y-2 text-xs text-muted-foreground bg-muted/30 p-3 rounded-xl border">
                <div className="flex items-center gap-1.5 font-bold text-foreground">
                  <ShieldCheck className="size-4 text-emerald-600" /> Instructions for Receptionist:
                </div>
                <p className="leading-relaxed">{activationData.instructions}</p>
                <p className="text-[11px] font-mono text-slate-600">
                  Patient portal activation link:{" "}
                  <strong>http://localhost:8080/patient/activate</strong>
                </p>
              </div>

              <DialogFooter className="flex gap-2 justify-end">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => {
                    navigator.clipboard.writeText(activationData.activationCode);
                    setCodeCopied(true);
                    toast.success("Activation code copied!");
                    setTimeout(() => setCodeCopied(false), 2000);
                  }}
                  className="rounded-xl gap-1.5"
                >
                  {codeCopied ? (
                    <>
                      <Check className="size-4 text-emerald-600" /> Copied
                    </>
                  ) : (
                    <>
                      <Copy className="size-4" /> Copy Code
                    </>
                  )}
                </Button>
                <Button
                  type="button"
                  onClick={() => setActivationModalOpen(false)}
                  className="rounded-xl font-bold"
                >
                  Done
                </Button>
              </DialogFooter>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}
