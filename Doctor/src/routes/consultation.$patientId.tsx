import { createFileRoute, Link, notFound, useNavigate } from "@tanstack/react-router";
import { useState, useEffect, useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { motion } from "motion/react";
import {
  AlertTriangle,
  ArrowLeft,
  BedDouble,
  CheckCircle2,
  ClipboardList,
  FileBadge,
  FlaskConical,
  Mic,
  MicOff,
  Printer,
  Save,
  Send,
  Sparkles,
  UserPlus,
  CalendarClock,
  Loader2,
  Video,
} from "lucide-react";
import { toast } from "sonner";

import { AppShell } from "@/components/layout/app-shell";
import { PatientProfileCard } from "@/components/consultation/patient-profile";
import { VitalsGrid } from "@/components/consultation/vitals-grid";
import { PrescriptionBuilder } from "@/components/consultation/prescription-builder";
import { PatientPrescriptionHistory } from "@/components/consultation/patient-prescription-history";
import { InvoiceBuilder } from "@/components/consultation/invoice-builder";
import { PatientBillingHistory } from "@/components/consultation/patient-billing-history";
import { PatientTimeline } from "@/components/consultation/patient-timeline";
import { AIClinicalAssistant } from "@/components/consultation/ai-clinical-assistant";
import { LabOrderForm } from "@/components/laboratory/lab-order-form";
import { PatientLabHistory } from "@/components/laboratory/patient-lab-history";
import { VideoConsultationCard } from "@/components/consultation/video-consultation-card";
import { findPatient, labCatalog } from "@/lib/healthcare-data";
import { patientApi } from "@/services/patient-api";
import { appointmentApi, type AppointmentResponse } from "@/services/appointment-api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { cn } from "@/lib/utils";
import { isAuthenticated, isDoctor } from "@/lib/auth";
import { consultationApi, ConsultationResponse } from "@/services/consultation-api";

export const Route = createFileRoute("/consultation/$patientId")({
  validateSearch: (
    search: Record<string, unknown>,
  ): { appointmentId?: number | undefined; queueEntryId?: number | undefined } => ({
    appointmentId: search["appointmentId"] ? Number(search["appointmentId"]) : undefined,
    queueEntryId: search["queueEntryId"] ? Number(search["queueEntryId"]) : undefined,
  }),
  head: () => ({
    meta: [
      { title: "Consultation Workspace — Meridian Health OS" },
      {
        name: "description",
        content:
          "Single-screen consultation workspace with vitals, diagnosis, digital prescription, labs and AI clinical copilot.",
      },
      { property: "og:title", content: "Consultation Workspace — Meridian Health OS" },
      {
        property: "og:description",
        content: "Complete an entire patient consultation from one AI-assisted workspace.",
      },
    ],
  }),
  loader: ({ params }) => {
    const patient = findPatient(params.patientId);
    if (!patient) throw notFound();
    return { patientId: patient.id };
  },
  component: ConsultationPage,
});

function EmergencyBanner({ alerts }: { alerts: string[] }) {
  if (!alerts.length) return null;
  return (
    <motion.div
      initial={{ opacity: 0, y: -8 }}
      animate={{ opacity: 1, y: 0 }}
      className="flex flex-wrap items-center gap-3 rounded-2xl border border-destructive/30 bg-destructive/8 px-4 py-3"
    >
      <span className="grid size-9 shrink-0 place-items-center rounded-xl bg-destructive/15 text-destructive">
        <AlertTriangle className="size-4" />
      </span>
      <div className="min-w-0 flex-1">
        <p className="text-sm font-bold text-destructive">Critical alerts active</p>
        <p className="truncate text-xs text-destructive/85">{alerts.join(" · ")}</p>
      </div>
      <Badge className="shrink-0 rounded-full border-0 bg-destructive text-[10px] text-destructive-foreground">
        Review before prescribing
      </Badge>
    </motion.div>
  );
}

interface DiagnosisFormProps {
  primaryDiagnosis: string;
  setPrimaryDiagnosis: (v: string) => void;
  secondaryDiagnosis: string;
  setSecondaryDiagnosis: (v: string) => void;
  symptoms: string;
  setSymptoms: (v: string) => void;
  clinicalNotes: string;
  setClinicalNotes: (v: string) => void;
  treatmentAdvice: string;
  setTreatmentAdvice: (v: string) => void;
}

function DiagnosisForm({
  primaryDiagnosis,
  setPrimaryDiagnosis,
  secondaryDiagnosis,
  setSecondaryDiagnosis,
  symptoms,
  setSymptoms,
  clinicalNotes,
  setClinicalNotes,
  treatmentAdvice,
  setTreatmentAdvice,
}: DiagnosisFormProps) {
  const [listening, setListening] = useState(false);

  const toggleMic = () => {
    setListening((l) => !l);
    if (!listening) {
      toast.info("Voice dictation started — speak your clinical notes");
      setTimeout(() => {
        setClinicalNotes(
          clinicalNotes
            ? `${clinicalNotes}\nPatient reports increased fatigue and nocturia over the last 3 weeks. Compliance good.`
            : "Patient reports increased fatigue and nocturia over the last 3 weeks. Compliance good.",
        );
        setListening(false);
      }, 1600);
    }
  };

  return (
    <section className="surface-card rounded-2xl p-4 sm:p-5">
      <div className="grid grid-cols-[minmax(0,1fr)_auto] items-center gap-3 sm:flex sm:justify-between">
        <h3 className="flex items-center gap-2 truncate text-sm font-bold">
          <ClipboardList className="size-4 text-primary" /> Diagnosis & Clinical Findings
        </h3>
        <Button
          size="sm"
          variant={listening ? "destructive" : "outline"}
          className="shrink-0 rounded-xl"
          onClick={toggleMic}
        >
          {listening ? <MicOff className="size-3.5" /> : <Mic className="size-3.5" />}
          {listening ? "Listening…" : "Dictate"}
        </Button>
      </div>

      <div className="mt-4 grid gap-3 sm:grid-cols-2">
        <div className="space-y-1.5">
          <Label className="text-xs">Primary diagnosis</Label>
          <Input
            value={primaryDiagnosis}
            onChange={(e) => setPrimaryDiagnosis(e.target.value)}
            placeholder="Type 2 Diabetes Mellitus — uncontrolled"
            className="rounded-xl"
          />
        </div>
        <div className="space-y-1.5">
          <Label className="text-xs">Secondary diagnosis</Label>
          <Input
            value={secondaryDiagnosis}
            onChange={(e) => setSecondaryDiagnosis(e.target.value)}
            placeholder="Stage 1 Hypertension"
            className="rounded-xl"
          />
        </div>
        <div className="space-y-1.5">
          <Label className="text-xs">Symptoms</Label>
          <Input
            value={symptoms}
            onChange={(e) => setSymptoms(e.target.value)}
            placeholder="Polyuria, fatigue…"
            className="rounded-xl"
          />
        </div>
        <div className="space-y-1.5">
          <Label className="text-xs">Treatment Advice / Plan</Label>
          <Input
            value={treatmentAdvice}
            onChange={(e) => setTreatmentAdvice(e.target.value)}
            placeholder="Low sodium diet, Metformin 500mg BD"
            className="rounded-xl"
          />
        </div>
        <div className="space-y-1.5 sm:col-span-2">
          <Label className="text-xs">Clinical findings & observations</Label>
          <Textarea
            value={clinicalNotes}
            onChange={(e) => setClinicalNotes(e.target.value)}
            placeholder="Systemic examination, general condition, local findings…"
            className={cn("min-h-24 rounded-xl", listening && "border-destructive/50")}
          />
        </div>
      </div>
    </section>
  );
}

function LabOrders() {
  const [selected, setSelected] = useState<string[]>(["HbA1c", "Lipid Profile"]);
  const toggle = (t: string) =>
    setSelected((s) => (s.includes(t) ? s.filter((x) => x !== t) : [...s, t]));

  return (
    <section className="surface-card rounded-2xl p-4 sm:p-5">
      <h3 className="flex items-center gap-2 text-sm font-bold">
        <FlaskConical className="size-4 text-primary" /> Laboratory & Imaging Orders
      </h3>
      <div className="mt-4 flex flex-wrap gap-2">
        {labCatalog.map((t) => (
          <button
            key={t}
            onClick={() => toggle(t)}
            className={cn(
              "rounded-full border px-3 py-1.5 text-xs font-medium transition-colors",
              selected.includes(t)
                ? "border-primary bg-primary text-primary-foreground"
                : "bg-background text-muted-foreground hover:border-primary/40 hover:text-foreground",
            )}
          >
            {t}
          </button>
        ))}
      </div>
      <div className="mt-3 flex flex-wrap items-center gap-2">
        <Input placeholder="Add custom test…" className="h-9 w-56 rounded-xl text-xs" />
        <Button
          size="sm"
          className="rounded-xl"
          onClick={() => toast.success(`${selected.length} tests ordered`)}
        >
          Order {selected.length} tests
        </Button>
      </div>
    </section>
  );
}

function FollowUp() {
  return (
    <section className="surface-card rounded-2xl p-4 sm:p-5">
      <h3 className="flex items-center gap-2 text-sm font-bold">
        <CalendarClock className="size-4 text-primary" /> Follow-up Plan
      </h3>
      <div className="mt-4 grid gap-3 sm:grid-cols-2">
        <div className="space-y-1.5">
          <Label className="text-xs">Next visit date</Label>
          <Input type="date" defaultValue="2026-08-28" className="rounded-xl" />
        </div>
        <div className="space-y-1.5">
          <Label className="text-xs">Reason</Label>
          <Input defaultValue="Review HbA1c & BP control" className="rounded-xl" />
        </div>
        <div className="space-y-1.5">
          <Label className="text-xs">Priority</Label>
          <div className="flex gap-2">
            {["Routine", "Urgent", "Critical"].map((p, i) => (
              <Badge
                key={p}
                variant={i === 1 ? "default" : "secondary"}
                className="cursor-pointer rounded-full px-3 py-1 text-[11px]"
              >
                {p}
              </Badge>
            ))}
          </div>
        </div>
        <div className="space-y-1.5">
          <Label className="text-xs">Follow-up notes</Label>
          <Input placeholder="Bring fasting reports" className="rounded-xl" />
        </div>
      </div>
      <div className="mt-4 rounded-xl bg-primary-soft p-3">
        <p className="text-[10px] font-bold uppercase tracking-wide text-accent-foreground">
          Automatic reminders
        </p>
        <div className="mt-2 flex flex-wrap gap-1.5">
          {["Voice call", "SMS", "App notification"].map((c) => (
            <Badge
              key={c}
              className="rounded-full border-0 bg-card text-[11px] text-accent-foreground"
            >
              {c}
            </Badge>
          ))}
          {["7 days before", "3 days before", "1 day before"].map((c) => (
            <Badge key={c} variant="secondary" className="rounded-full text-[11px]">
              {c}
            </Badge>
          ))}
        </div>
      </div>
    </section>
  );
}

function ConsultationPage() {
  const navigate = useNavigate();
  const { patientId } = Route.useLoaderData();
  const { appointmentId, queueEntryId } = Route.useSearch();
  // Parse numeric patient ID if possible for backend REST API
  const numericPatientId = parseInt(patientId.replace(/\D/g, "")) || 0;

  // Query real patient details from backend if numeric ID
  const { data: realPatientData } = useQuery({
    queryKey: ["consultation-patient-profile", numericPatientId],
    queryFn: () => patientApi.getPatientById(numericPatientId),
    enabled: numericPatientId > 0 && isAuthenticated(),
  });

  // Query appointment details if appointmentId provided in search params
  const { data: appointmentData, refetch: refetchAppointment } = useQuery({
    queryKey: ["consultation-appointment", appointmentId],
    queryFn: () => appointmentApi.getAppointmentById(appointmentId!),
    enabled: Boolean(appointmentId && appointmentId > 0 && isAuthenticated()),
    refetchInterval: (query) => {
      const apt = query.state.data;
      return apt?.appointmentType === "TELECONSULTATION" &&
        apt.status !== "COMPLETED" &&
        apt.status !== "CANCELLED"
        ? 15000
        : false;
    },
  });

  // Fallback: if no appointmentId in search params, query recent appointments for this patient
  const { data: patientAppointments } = useQuery({
    queryKey: ["consultation-patient-appointments", numericPatientId],
    queryFn: () => appointmentApi.getPatientAppointments(numericPatientId, 0, 5),
    enabled: !appointmentId && numericPatientId > 0 && isAuthenticated(),
  });

  const activeAppointment: AppointmentResponse | undefined =
    appointmentData ||
    patientAppointments?.content?.find(
      (a) =>
        a.status === "IN_PROGRESS" ||
        a.status === "CONFIRMED" ||
        a.status === "CHECKED_IN" ||
        a.status === "BOOKED",
    );

  const fallbackPatient = findPatient(patientId)!;
  const patient = useMemo(() => {
    if (!realPatientData) return fallbackPatient;
    const p = realPatientData;
    const birthDate = p.dateOfBirth ? new Date(p.dateOfBirth) : null;
    const age =
      birthDate && !isNaN(birthDate.getTime())
        ? new Date().getFullYear() - birthDate.getFullYear()
        : fallbackPatient.age;
    const fullName = `${p.firstName} ${p.lastName}`.trim();
    const bg = p.bloodGroup
      ? p.bloodGroup
          .replace("_POSITIVE", "+")
          .replace("_NEGATIVE", "-")
          .replace("POSITIVE", "+")
          .replace("NEGATIVE", "-")
      : fallbackPatient.bloodGroup;
    const gender = p.gender
      ? ((p.gender.charAt(0).toUpperCase() + p.gender.slice(1).toLowerCase()) as "Male" | "Female")
      : fallbackPatient.gender;

    return {
      ...fallbackPatient,
      id: String(p.id),
      name: fullName || fallbackPatient.name,
      upid: p.patientCode || fallbackPatient.upid,
      mobile: p.mobileNumber || fallbackPatient.mobile,
      age: age > 0 ? age : fallbackPatient.age,
      gender,
      bloodGroup: bg,
      photo: `https://api.dicebear.com/9.x/notionists/svg?seed=${encodeURIComponent(
        p.firstName || "Patient",
      )}&backgroundColor=dbeafe,e0f2fe,ede9fe`,
    };
  }, [realPatientData, fallbackPatient]);

  const [aiOpen, setAiOpen] = useState(true);

  // Form states
  const [primaryDiagnosis, setPrimaryDiagnosis] = useState(
    "Type 2 Diabetes Mellitus — uncontrolled",
  );
  const [secondaryDiagnosis, setSecondaryDiagnosis] = useState("Stage 1 Hypertension");
  const [symptoms, setSymptoms] = useState("Polyuria, fatigue, mild headache");
  const [clinicalNotes, setClinicalNotes] = useState(
    "Patient reports increased fatigue over the last 3 weeks. Systemic exam normal, BP slightly elevated.",
  );
  const [treatmentAdvice, setTreatmentAdvice] = useState(
    "Low sodium diet, Metformin 500mg BD, Amlodipine 5mg QD",
  );

  const [activeConsultation, setActiveConsultation] = useState<ConsultationResponse | null>(null);
  const [saving, setSaving] = useState(false);
  const [completing, setCompleting] = useState(false);

  useEffect(() => {
    if (!isAuthenticated()) {
      navigate({ to: "/login" });
      return;
    }
    if (!isDoctor()) {
      toast.error("Unauthorized: Consultation workspace is doctor-only");
      navigate({ to: "/receptionist" });
      return;
    }
  }, [navigate]);

  useEffect(() => {
    if (!isDoctor()) return;
    let isMounted = true;
    async function loadBackendConsultation() {
      try {
        const history = await consultationApi.getPatientConsultations(numericPatientId);
        if (isMounted && history.content && history.content.length > 0) {
          const latest = history.content[0];
          if (latest) {
            setActiveConsultation(latest);
            if (latest.diagnosis) setPrimaryDiagnosis(latest.diagnosis);
            if (latest.symptoms) setSymptoms(latest.symptoms);
            if (latest.clinicalNotes) setClinicalNotes(latest.clinicalNotes);
            if (latest.treatmentAdvice) setTreatmentAdvice(latest.treatmentAdvice);
          }
        }
      } catch {
        // Fallback gracefully if patient history isn't seeded yet
      }
    }
    loadBackendConsultation();
    return () => {
      isMounted = false;
    };
  }, [numericPatientId]);

  const handleSaveDraft = async () => {
    setSaving(true);
    try {
      const fullDiagnosis = secondaryDiagnosis
        ? `${primaryDiagnosis} | ${secondaryDiagnosis}`
        : primaryDiagnosis;

      if (activeConsultation && activeConsultation.status === "IN_PROGRESS") {
        const updated = await consultationApi.updateConsultation(activeConsultation.id, {
          chiefComplaint: symptoms,
          symptoms,
          clinicalNotes,
          diagnosis: fullDiagnosis,
          treatmentAdvice,
        });
        setActiveConsultation(updated);
        toast.success("Consultation draft updated successfully");
      } else {
        const created = await consultationApi.createConsultation({
          patientId: numericPatientId,
          appointmentId,
          queueEntryId,
          chiefComplaint: symptoms,
          symptoms,
          clinicalNotes,
          diagnosis: fullDiagnosis,
          treatmentAdvice,
        });
        setActiveConsultation(created);
        toast.success("Consultation draft created successfully");
      }
    } catch (err: unknown) {
      toast.error(err instanceof Error ? err.message : "Failed to save consultation draft");
    } finally {
      setSaving(false);
    }
  };

  const handleCompleteConsultation = async () => {
    setCompleting(true);
    try {
      let currentId = activeConsultation?.id;
      const fullDiagnosis = secondaryDiagnosis
        ? `${primaryDiagnosis} | ${secondaryDiagnosis}`
        : primaryDiagnosis;

      if (!currentId || activeConsultation?.status !== "IN_PROGRESS") {
        const created = await consultationApi.createConsultation({
          patientId: numericPatientId,
          chiefComplaint: symptoms,
          symptoms,
          clinicalNotes,
          diagnosis: fullDiagnosis,
          treatmentAdvice,
        });
        currentId = created.id;
      } else {
        await consultationApi.updateConsultation(currentId, {
          chiefComplaint: symptoms,
          symptoms,
          clinicalNotes,
          diagnosis: fullDiagnosis,
          treatmentAdvice,
        });
      }

      const completed = await consultationApi.completeConsultation(currentId);
      setActiveConsultation(completed);
      toast.success("Consultation completed successfully!");
      setTimeout(() => {
        navigate({ to: "/queue" });
      }, 1000);
    } catch (err: unknown) {
      toast.error(err instanceof Error ? err.message : "Failed to complete consultation");
    } finally {
      setCompleting(false);
    }
  };

  return (
    <AppShell
      aiPanel={
        <AIClinicalAssistant
          patientId={numericPatientId}
          consultationId={activeConsultation?.id}
          open={aiOpen}
          onClose={() => setAiOpen(false)}
        />
      }
    >
      <div className="space-y-5 p-4 pb-28 sm:p-6">
        <div className="grid grid-cols-[minmax(0,1fr)_auto] items-center gap-3 sm:flex sm:justify-between">
          <div className="flex min-w-0 items-center gap-3">
            <Link to="/queue">
              <Button variant="ghost" size="icon" className="shrink-0 rounded-xl">
                <ArrowLeft className="size-4" />
              </Button>
            </Link>
            <div className="min-w-0">
              <div className="flex items-center gap-2">
                <h1 className="truncate text-lg font-extrabold sm:text-xl">
                  Consultation Workspace
                </h1>
                {activeConsultation && (
                  <Badge
                    variant={activeConsultation.status === "COMPLETED" ? "default" : "outline"}
                    className="rounded-full text-[10px]"
                  >
                    {activeConsultation.status}
                  </Badge>
                )}
                {activeAppointment?.appointmentType === "TELECONSULTATION" && (
                  <Badge className="bg-indigo-100 text-indigo-900 border-indigo-200 rounded-full text-[10px] font-bold">
                    <Video className="size-3 mr-1" /> Teleconsultation
                  </Badge>
                )}
              </div>
              <p className="num truncate text-xs text-muted-foreground">
                Token A-104 · {patient.upid}
              </p>
            </div>
          </div>
          <Button
            variant={aiOpen ? "default" : "outline"}
            size="sm"
            className="shrink-0 rounded-xl"
            onClick={() => setAiOpen((o) => !o)}
          >
            <Sparkles className="size-3.5" /> AI Copilot
          </Button>
        </div>

        <EmergencyBanner alerts={patient.alerts} />

        <div className="grid gap-5 lg:grid-cols-[320px_minmax(0,1fr)]">
          <div className="space-y-5">
            <PatientProfileCard patient={patient} />
            <div className="hidden lg:block">
              <PatientTimeline visits={patient.timeline} />
            </div>
          </div>

          <div className="min-w-0 space-y-5">
            {activeAppointment && activeAppointment.appointmentType === "TELECONSULTATION" && (
              <VideoConsultationCard
                appointment={activeAppointment}
                onAppointmentUpdated={() => refetchAppointment()}
              />
            )}

            <Tabs defaultValue="consult">
              <TabsList className="w-full justify-start overflow-x-auto rounded-xl">
                <TabsTrigger value="consult" className="rounded-lg text-xs">
                  Consultation
                </TabsTrigger>
                <TabsTrigger value="rx" className="rounded-lg text-xs">
                  Prescription
                </TabsTrigger>
                <TabsTrigger value="billing" className="rounded-lg text-xs">
                  Billing
                </TabsTrigger>
                <TabsTrigger value="labs" className="rounded-lg text-xs">
                  Labs & Follow-up
                </TabsTrigger>
                <TabsTrigger value="history" className="rounded-lg text-xs lg:hidden">
                  History
                </TabsTrigger>
              </TabsList>

              <TabsContent value="consult" className="mt-4 space-y-5">
                <VitalsGrid vitals={patient.vitals} />
                <DiagnosisForm
                  primaryDiagnosis={primaryDiagnosis}
                  setPrimaryDiagnosis={setPrimaryDiagnosis}
                  secondaryDiagnosis={secondaryDiagnosis}
                  setSecondaryDiagnosis={setSecondaryDiagnosis}
                  symptoms={symptoms}
                  setSymptoms={setSymptoms}
                  clinicalNotes={clinicalNotes}
                  setClinicalNotes={setClinicalNotes}
                  treatmentAdvice={treatmentAdvice}
                  setTreatmentAdvice={setTreatmentAdvice}
                />
              </TabsContent>
              <TabsContent value="rx" className="mt-4 space-y-5">
                <PrescriptionBuilder
                  consultationId={activeConsultation?.id}
                  allergies={patient.allergies}
                />
                <PatientPrescriptionHistory patientId={numericPatientId} />
              </TabsContent>
              <TabsContent value="billing" className="mt-4 space-y-5">
                <InvoiceBuilder
                  consultationId={activeConsultation?.id}
                  patientId={numericPatientId}
                  doctorId={activeConsultation?.doctorId}
                  appointmentId={activeConsultation?.appointmentId || undefined}
                />
                <PatientBillingHistory patientId={numericPatientId} />
              </TabsContent>
              <TabsContent value="labs" className="mt-4 space-y-5">
                <LabOrderForm
                  initialPatientId={numericPatientId}
                  initialConsultationId={activeConsultation?.id}
                />
                <PatientLabHistory patientId={numericPatientId} />
                <FollowUp />
              </TabsContent>
              <TabsContent value="history" className="mt-4 lg:hidden">
                <PatientTimeline visits={patient.timeline} />
              </TabsContent>
            </Tabs>
          </div>
        </div>
      </div>

      <div className="glass-panel fixed inset-x-0 bottom-0 z-30 rounded-none border-x-0 border-b-0 px-4 py-3">
        <div className="flex gap-2 overflow-x-auto scroll-slim">
          <Button
            size="sm"
            variant="outline"
            disabled={saving || activeConsultation?.status === "COMPLETED"}
            className="shrink-0 rounded-xl"
            onClick={handleSaveDraft}
          >
            {saving ? <Loader2 className="size-3.5 animate-spin" /> : <Save className="size-3.5" />}
            Save Draft
          </Button>

          <Button
            size="sm"
            variant="default"
            disabled={completing || activeConsultation?.status === "COMPLETED"}
            className="shrink-0 rounded-xl"
            onClick={handleCompleteConsultation}
          >
            {completing ? (
              <Loader2 className="size-3.5 animate-spin" />
            ) : (
              <CheckCircle2 className="size-3.5" />
            )}
            Complete Consultation
          </Button>

          <Button
            size="sm"
            variant="outline"
            className="shrink-0 rounded-xl"
            onClick={() => toast.success("Prescription Printed")}
          >
            <Printer className="size-3.5" /> Print Prescription
          </Button>

          <Button
            size="sm"
            variant="outline"
            className="shrink-0 rounded-xl"
            onClick={() => toast.success("Prescription Sent to Patient")}
          >
            <Send className="size-3.5" /> Send Prescription
          </Button>

          <Button
            size="sm"
            variant="outline"
            className="shrink-0 rounded-xl"
            onClick={() => toast.success("Lab Order Submitted")}
          >
            <FlaskConical className="size-3.5" /> Order Lab
          </Button>

          <Button
            size="sm"
            variant="outline"
            className="shrink-0 rounded-xl"
            onClick={() => toast.success("Specialist Referral Initiated")}
          >
            <UserPlus className="size-3.5" /> Refer Specialist
          </Button>

          <Button
            size="sm"
            variant="outline"
            className="shrink-0 rounded-xl"
            onClick={() => toast.success("Admission Order Created")}
          >
            <BedDouble className="size-3.5" /> Hospital Admission
          </Button>

          <Button
            size="sm"
            variant="outline"
            className="shrink-0 rounded-xl"
            onClick={() => toast.success("Medical Certificate Generated")}
          >
            <FileBadge className="size-3.5" /> Medical Certificate
          </Button>
        </div>
      </div>
    </AppShell>
  );
}
