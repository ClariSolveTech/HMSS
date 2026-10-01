import { useState, useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { FlaskConical, AlertCircle, CheckCircle2, Loader2, Plus, Trash2, User, FileText } from "lucide-react";
import { toast } from "sonner";

import { apiClient } from "@/lib/api-client";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { PaginatedResponse } from "@/services/consultation-api";
import {
  useLabTests,
  useCreateLabOrder,
  LabTestResponse,
  LabOrderPriority,
  CreateLabOrderRequest,
} from "@/services/lab-api";

export interface PatientSummary {
  id: number;
  patientCode: string;
  firstName: string;
  lastName: string;
  gender?: string | undefined;
  mobileNumber?: string | undefined;
  dateOfBirth?: string | undefined;
}

export interface LabOrderFormProps {
  initialPatientId?: number | undefined;
  initialConsultationId?: number | undefined;
  onSuccess?: (() => void) | undefined;
}

export function LabOrderForm({ initialPatientId, initialConsultationId, onSuccess }: LabOrderFormProps) {
  const [selectedPatientId, setSelectedPatientId] = useState<number | null>(initialPatientId || null);
  const [consultationId, setConsultationId] = useState<number | undefined>(initialConsultationId);
  const [priority, setPriority] = useState<LabOrderPriority>("NORMAL");
  const [clinicalNotes, setClinicalNotes] = useState<string>("");
  const [selectedItems, setSelectedItems] = useState<{ labTestId: number; notes: string }[]>([]);

  // Fetch Patients
  const { data: patientsData, isLoading: loadingPatients } = useQuery({
    queryKey: ["patients-list"],
    queryFn: () => apiClient.get<PaginatedResponse<PatientSummary>>("/api/v1/patients?size=100"),
  });

  // Fetch Lab Catalog Tests
  const { data: labTests, isLoading: loadingTests, isError: testsError } = useLabTests();

  const createOrderMutation = useCreateLabOrder();

  useEffect(() => {
    if (initialPatientId) {
      setSelectedPatientId(initialPatientId);
    }
  }, [initialPatientId]);

  useEffect(() => {
    if (initialConsultationId) {
      setConsultationId(initialConsultationId);
    }
  }, [initialConsultationId]);

  const selectedPatient = patientsData?.content.find((p) => p.id === selectedPatientId);

  const toggleTest = (testId: number) => {
    if (selectedItems.some((item) => item.labTestId === testId)) {
      setSelectedItems((prev) => prev.filter((item) => item.labTestId !== testId));
    } else {
      setSelectedItems((prev) => [...prev, { labTestId: testId, notes: "" }]);
    }
  };

  const updateItemNotes = (testId: number, notes: string) => {
    setSelectedItems((prev) =>
      prev.map((item) => (item.labTestId === testId ? { ...item, notes } : item))
    );
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!selectedPatientId) {
      toast.error("Please select a patient.");
      return;
    }

    if (selectedItems.length === 0) {
      toast.error("Please select at least one laboratory test.");
      return;
    }

    try {
      const payload: CreateLabOrderRequest = {
        patientId: selectedPatientId,
        priority,
        items: selectedItems.map((item) => ({
          labTestId: item.labTestId,
          ...(item.notes.trim() ? { notes: item.notes.trim() } : {}),
        })),
        ...(consultationId ? { consultationId } : {}),
        ...(clinicalNotes.trim() ? { clinicalNotes: clinicalNotes.trim() } : {}),
      };

      const res = await createOrderMutation.mutateAsync(payload);
      toast.success(`Lab Order ${res.orderNumber} created successfully!`);

      // Reset form if not pre-locked to a patient
      if (!initialPatientId) {
        setSelectedPatientId(null);
      }
      setSelectedItems([]);
      setClinicalNotes("");
      setPriority("NORMAL");

      if (onSuccess) {
        onSuccess();
      }
    } catch (err: any) {
      toast.error(err.message || "Failed to create lab order.");
    }
  };

  return (
    <form onSubmit={handleSubmit} className="surface-card space-y-6 rounded-2xl p-4 sm:p-6">
      <div className="flex items-center justify-between border-b pb-4">
        <div className="flex items-center gap-2.5">
          <span className="grid size-9 place-items-center rounded-xl bg-primary/10 text-primary">
            <FlaskConical className="size-4" />
          </span>
          <div>
            <h3 className="text-base font-extrabold">Create Laboratory Order</h3>
            <p className="text-xs text-muted-foreground">Order diagnostic tests for hospital patient</p>
          </div>
        </div>
        <Badge variant="outline" className="rounded-full text-[11px]">
          Priority: {priority}
        </Badge>
      </div>

      {/* 1. Patient Selection */}
      <div className="space-y-2">
        <Label className="text-xs font-bold">Select Patient *</Label>
        {initialPatientId && selectedPatient ? (
          <div className="flex items-center gap-3 rounded-xl border bg-muted/30 p-3">
            <User className="size-4 text-primary" />
            <div className="min-w-0 flex-1">
              <p className="text-sm font-semibold">
                {selectedPatient.firstName} {selectedPatient.lastName}
              </p>
              <p className="num text-xs text-muted-foreground">
                Code: {selectedPatient.patientCode} {selectedPatient.gender ? `· ${selectedPatient.gender}` : ""}
              </p>
            </div>
            <Badge variant="secondary" className="rounded-full text-[10px]">
              Pre-selected
            </Badge>
          </div>
        ) : (
          <select
            value={selectedPatientId || ""}
            onChange={(e) => setSelectedPatientId(e.target.value ? Number(e.target.value) : null)}
            className="w-full rounded-xl border bg-background px-3 py-2.5 text-xs font-medium focus:outline-none focus:ring-2 focus:ring-primary/40"
            disabled={loadingPatients}
          >
            <option value="">-- Choose Patient --</option>
            {patientsData?.content.map((p) => (
              <option key={p.id} value={p.id}>
                {p.firstName} {p.lastName} ({p.patientCode})
              </option>
            ))}
          </select>
        )}
        {selectedPatient && !initialPatientId && (
          <div className="rounded-lg bg-primary/5 p-2.5 text-xs text-muted-foreground">
            Selected: <span className="font-semibold text-foreground">{selectedPatient.firstName} {selectedPatient.lastName}</span> ({selectedPatient.patientCode})
          </div>
        )}
      </div>

      {/* 2. Priority & Consultation Link */}
      <div className="grid gap-4 sm:grid-cols-2">
        <div className="space-y-2">
          <Label className="text-xs font-bold">Priority *</Label>
          <div className="flex gap-2">
            {(["NORMAL", "URGENT", "STAT"] as LabOrderPriority[]).map((p) => (
              <button
                type="button"
                key={p}
                onClick={() => setPriority(p)}
                className={`flex-1 rounded-xl border py-2 text-xs font-bold transition-all ${
                  priority === p
                    ? p === "STAT"
                      ? "border-destructive bg-destructive text-destructive-foreground"
                      : p === "URGENT"
                      ? "border-warning bg-warning text-warning-foreground"
                      : "border-primary bg-primary text-primary-foreground"
                    : "bg-background hover:bg-accent"
                }`}
              >
                {p}
              </button>
            ))}
          </div>
        </div>

        <div className="space-y-2">
          <Label className="text-xs font-bold">Consultation Link</Label>
          <Input
            type="text"
            readOnly
            value={consultationId ? `Consultation #${consultationId}` : "Standalone (Direct Order)"}
            className="h-10 rounded-xl bg-muted/40 text-xs font-medium text-muted-foreground"
          />
        </div>
      </div>

      {/* 3. Clinical Notes */}
      <div className="space-y-2">
        <Label className="text-xs font-bold">Clinical Notes / Indications</Label>
        <Textarea
          placeholder="e.g. Fasting sample required, pre-op investigation, suspected anemia..."
          value={clinicalNotes}
          onChange={(e) => setClinicalNotes(e.target.value)}
          rows={2}
          className="rounded-xl text-xs"
        />
      </div>

      {/* 4. Test Selection Catalog */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <Label className="text-xs font-bold">Select Laboratory Tests *</Label>
          <span className="text-xs text-muted-foreground">
            {selectedItems.length} test{selectedItems.length === 1 ? "" : "s"} selected
          </span>
        </div>

        {loadingTests ? (
          <div className="flex items-center justify-center py-6 text-xs text-muted-foreground">
            <Loader2 className="mr-2 size-4 animate-spin text-primary" /> Loading available lab catalog...
          </div>
        ) : testsError ? (
          <div className="rounded-xl border border-destructive/20 bg-destructive/5 p-4 text-xs text-destructive">
            <AlertCircle className="mr-1.5 inline size-4" /> Unable to load laboratory catalog.
          </div>
        ) : !labTests || labTests.length === 0 ? (
          <div className="rounded-xl border border-dashed p-6 text-center text-xs text-muted-foreground">
            No laboratory tests found in hospital catalog.
          </div>
        ) : (
          <div className="grid gap-2.5 sm:grid-cols-2">
            {labTests.filter((t) => t.active).map((test) => {
              const isSelected = selectedItems.some((item) => item.labTestId === test.id);
              return (
                <div
                  key={test.id}
                  onClick={() => toggleTest(test.id)}
                  className={`group relative cursor-pointer rounded-xl border p-3 transition-all ${
                    isSelected
                      ? "border-primary bg-primary/8 shadow-sm"
                      : "bg-background hover:border-primary/40"
                  }`}
                >
                  <div className="flex items-start justify-between gap-2">
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-1.5">
                        <span className="font-semibold text-xs text-foreground group-hover:text-primary">
                          {test.testName}
                        </span>
                        <Badge variant="outline" className="rounded-md px-1.5 py-0 text-[10px]">
                          {test.testCode}
                        </Badge>
                      </div>
                      <p className="mt-1 text-[11px] text-muted-foreground">
                        {test.category || "General"} {test.sampleType ? `· ${test.sampleType}` : ""}
                      </p>
                      {test.referenceRange && (
                        <p className="mt-0.5 text-[10px] text-muted-foreground/80">
                          Ref: {test.referenceRange} {test.unit || ""}
                        </p>
                      )}
                    </div>
                    <span
                      className={`grid size-5 shrink-0 place-items-center rounded-md border text-xs ${
                        isSelected ? "border-primary bg-primary text-primary-foreground" : "border-muted-foreground/30"
                      }`}
                    >
                      {isSelected ? "✓" : ""}
                    </span>
                  </div>

                  {/* Notes per test if selected */}
                  {isSelected && (
                    <div className="mt-2.5 border-t border-primary/20 pt-2" onClick={(e) => e.stopPropagation()}>
                      <Input
                        placeholder="Test specific instruction (optional)..."
                        value={selectedItems.find((i) => i.labTestId === test.id)?.notes || ""}
                        onChange={(e) => updateItemNotes(test.id, e.target.value)}
                        className="h-7 rounded-lg text-[11px]"
                      />
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* 5. Order Summary & Submit */}
      {selectedItems.length > 0 && (
        <div className="rounded-xl border bg-muted/20 p-4 space-y-2">
          <p className="text-xs font-bold text-foreground">Order Summary</p>
          <div className="flex flex-wrap gap-1.5">
            {selectedItems.map((item) => {
              const test = labTests?.find((t) => t.id === item.labTestId);
              return (
                <Badge key={item.labTestId} variant="secondary" className="rounded-lg text-[11px] py-1 px-2">
                  {test?.testName || `Test #${item.labTestId}`} ({test?.testCode})
                </Badge>
              );
            })}
          </div>
        </div>
      )}

      <Button
        type="submit"
        disabled={createOrderMutation.isPending || !selectedPatientId || selectedItems.length === 0}
        className="w-full rounded-xl py-2.5 font-bold"
      >
        {createOrderMutation.isPending ? (
          <>
            <Loader2 className="mr-2 size-4 animate-spin" /> Submitting Order...
          </>
        ) : (
          <>
            <FlaskConical className="mr-2 size-4" /> Submit Lab Order ({selectedItems.length} test{selectedItems.length === 1 ? "" : "s"})
          </>
        )}
      </Button>
    </form>
  );
}
