import { useMemo, useState, useEffect } from "react";
import { AnimatePresence, motion } from "motion/react";
import {
  BellRing,
  Check,
  PhoneCall,
  Pill,
  Plus,
  RefreshCw,
  Search,
  Trash2,
  X,
  Save,
  Send,
  Loader2,
  AlertCircle,
  CheckCircle,
} from "lucide-react";
import { toast } from "sonner";

import { medicineCatalog } from "@/lib/healthcare-data";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { cn } from "@/lib/utils";
import {
  prescriptionApi,
  PrescriptionItemRequest,
  PrescriptionResponse,
} from "@/services/prescription-api";
import { PrescriptionView } from "./prescription-view";

interface PrescriptionBuilderProps {
  consultationId?: number | undefined;
  allergies?: string[];
  onPrescriptionUpdated?: (rx: PrescriptionResponse) => void;
}

export function PrescriptionBuilder({
  consultationId,
  allergies = [],
  onPrescriptionUpdated,
}: PrescriptionBuilderProps) {
  const [prescription, setPrescription] = useState<PrescriptionResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [issuing, setIssuing] = useState(false);
  const [cancelling, setCancelling] = useState(false);

  // Form states
  const [items, setItems] = useState<PrescriptionItemRequest[]>([
    {
      medicineName: "Paracetamol",
      dosage: "500 mg",
      frequency: "Twice daily",
      duration: "5 days",
      route: "ORAL",
      instructions: "After food",
    },
  ]);
  const [notes, setNotes] = useState("Take medicines with warm water after meals.");
  const [query, setQuery] = useState("");

  // Modals for lifecycle actions
  const [showIssueConfirm, setShowIssueConfirm] = useState(false);
  const [showCancelConfirm, setShowCancelConfirm] = useState(false);

  useEffect(() => {
    let isMounted = true;
    async function loadExistingPrescription() {
      if (!consultationId) return;
      setLoading(true);
      try {
        // Fetch patient prescriptions or doctor prescriptions to check if consultation has prescription
        const res = await prescriptionApi.getDoctorOwnPrescriptions(0, 50);
        const match = res.content.find((p) => p.consultationId === consultationId);
        if (isMounted && match) {
          setPrescription(match);
          setNotes(match.notes || "");
          setItems(
            match.items.map((i) => ({
              medicineName: i.medicineName,
              dosage: i.dosage,
              frequency: i.frequency,
              duration: i.duration,
              route: i.route || "ORAL",
              instructions: i.instructions || "",
            }))
          );
          if (onPrescriptionUpdated) onPrescriptionUpdated(match);
        }
      } catch (err) {
        // Silent catch if no prescription exists yet
      } finally {
        if (isMounted) setLoading(false);
      }
    }
    loadExistingPrescription();
    return () => {
      isMounted = false;
    };
  }, [consultationId]);

  const suggestions = useMemo(() => {
    const t = query.trim().toLowerCase();
    if (!t) return [];
    return medicineCatalog.filter((m) => m.name.toLowerCase().includes(t)).slice(0, 5);
  }, [query]);

  const addMed = (name: string, dosage: string) => {
    setItems((prev) => [
      ...prev,
      {
        medicineName: name,
        dosage,
        frequency: "Twice daily",
        duration: "5 days",
        route: "ORAL",
        instructions: "After food",
      },
    ]);
    setQuery("");
  };

  const removeMed = (index: number) => {
    setItems((prev) => prev.filter((_, i) => i !== index));
  };

  const updateItem = (index: number, patch: Partial<PrescriptionItemRequest>) => {
    setItems((prev) =>
      prev.map((item, i) => (i === index ? { ...item, ...patch } : item))
    );
  };

  const allergyRisk = (name: string) =>
    allergies.some((a) => a.toLowerCase().includes(name.toLowerCase().slice(0, 4)));

  const validateItems = (): boolean => {
    if (items.length === 0) {
      toast.error("Prescription must contain at least one medicine item.");
      return false;
    }
    for (let i = 0; i < items.length; i++) {
      const it = items[i];
      if (!it) continue;
      if (!it.medicineName.trim()) {
        toast.error(`Medicine #${i + 1}: Name is required.`);
        return false;
      }
      if (!it.dosage.trim()) {
        toast.error(`Medicine #${i + 1} (${it.medicineName}): Dosage is required.`);
        return false;
      }
      if (!it.frequency.trim()) {
        toast.error(`Medicine #${i + 1} (${it.medicineName}): Frequency is required.`);
        return false;
      }
      if (!it.duration.trim()) {
        toast.error(`Medicine #${i + 1} (${it.medicineName}): Duration is required.`);
        return false;
      }
    }
    return true;
  };

  const handleSaveDraft = async () => {
    if (!validateItems()) return;
    if (!consultationId) {
      toast.error("Consultation ID is missing. Please save consultation first.");
      return;
    }

    setSaving(true);
    try {
      if (prescription) {
        // Update existing draft
        const updated = await prescriptionApi.updatePrescription(prescription.id, {
          notes,
          items,
        });
        setPrescription(updated);
        toast.success("Prescription draft updated successfully");
        if (onPrescriptionUpdated) onPrescriptionUpdated(updated);
      } else {
        // Create new draft
        const created = await prescriptionApi.createPrescription({
          consultationId,
          notes,
          items,
        });
        setPrescription(created);
        toast.success(`Prescription ${created.prescriptionCode} created in DRAFT state`);
        if (onPrescriptionUpdated) onPrescriptionUpdated(created);
      }
    } catch (err: any) {
      if (err.status === 409) {
        toast.info("Prescription already exists. Fetching latest...");
        try {
          const res = await prescriptionApi.getDoctorOwnPrescriptions(0, 50);
          const match = res.content.find((p) => p.consultationId === consultationId);
          if (match) {
            setPrescription(match);
            if (onPrescriptionUpdated) onPrescriptionUpdated(match);
          }
        } catch {}
      } else {
        toast.error(err.message || "Failed to save prescription draft");
      }
    } finally {
      setSaving(false);
    }
  };

  const handleIssueConfirm = async () => {
    setShowIssueConfirm(false);
    if (!prescription) {
      // Create first then issue
      if (!validateItems()) return;
      if (!consultationId) return;
      setIssuing(true);
      try {
        const created = await prescriptionApi.createPrescription({
          consultationId,
          notes,
          items,
        });
        const issued = await prescriptionApi.issuePrescription(created.id);
        setPrescription(issued);
        toast.success(`Prescription ${issued.prescriptionCode} ISSUED successfully!`);
        if (onPrescriptionUpdated) onPrescriptionUpdated(issued);
      } catch (err: any) {
        toast.error(err.message || "Failed to issue prescription");
      } finally {
        setIssuing(false);
      }
      return;
    }

    setIssuing(true);
    try {
      // Update draft first to reflect form state
      await prescriptionApi.updatePrescription(prescription.id, { notes, items });
      const issued = await prescriptionApi.issuePrescription(prescription.id);
      setPrescription(issued);
      toast.success(`Prescription ${issued.prescriptionCode} ISSUED successfully!`);
      if (onPrescriptionUpdated) onPrescriptionUpdated(issued);
    } catch (err: any) {
      toast.error(err.message || "Failed to issue prescription");
    } finally {
      setIssuing(false);
    }
  };

  const handleCancelConfirm = async () => {
    setShowCancelConfirm(false);
    if (!prescription) return;
    setCancelling(true);
    try {
      const cancelled = await prescriptionApi.cancelPrescription(prescription.id);
      setPrescription(cancelled);
      toast.success(`Prescription ${cancelled.prescriptionCode} CANCELLED`);
      if (onPrescriptionUpdated) onPrescriptionUpdated(cancelled);
    } catch (err: any) {
      toast.error(err.message || "Failed to cancel prescription");
    } finally {
      setCancelling(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-12 surface-card rounded-2xl">
        <Loader2 className="size-6 animate-spin text-primary mr-2" />
        <span className="text-sm font-semibold">Loading prescription data...</span>
      </div>
    );
  }

  // If prescription is ISSUED or CANCELLED, display read-only PrescriptionView
  if (prescription && prescription.status !== "DRAFT") {
    return (
      <div className="space-y-4">
        <div className="rounded-xl border border-primary/20 bg-primary/5 p-3 flex items-center gap-2 text-xs font-semibold">
          <CheckCircle className="size-4 text-primary shrink-0" />
          <span>
            This prescription has been {prescription.status}. Editing is disabled.
          </span>
        </div>
        <PrescriptionView prescription={prescription} />
      </div>
    );
  }

  return (
    <section className="surface-card rounded-2xl p-4 sm:p-5">
      <div className="grid grid-cols-[minmax(0,1fr)_auto] items-center gap-3 sm:flex sm:justify-between">
        <div className="min-w-0">
          <div className="flex items-center gap-2">
            <h3 className="flex items-center gap-2 truncate text-sm font-bold">
              <Pill className="size-4 text-primary" /> Digital Prescription
            </h3>
            {prescription ? (
              <Badge variant="outline" className="rounded-full text-[10px]">
                {prescription.prescriptionCode} (DRAFT)
              </Badge>
            ) : (
              <Badge variant="secondary" className="rounded-full text-[10px]">
                NEW DRAFT
              </Badge>
            )}
          </div>
          <p className="truncate text-xs text-muted-foreground mt-0.5">
            {items.length} medicine{items.length === 1 ? "" : "s"} · e-signed by consulting doctor
          </p>
        </div>
        <Badge variant="secondary" className="shrink-0 rounded-full text-[10px]">
          Rx
        </Badge>
      </div>

      <div className="relative mt-4">
        <Search className="pointer-events-none absolute left-3.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search medicine catalog (e.g. Paracetamol, Metformin, Azithromycin)…"
          className="h-11 rounded-xl pl-10"
        />
        <AnimatePresence>
          {suggestions.length > 0 && (
            <motion.ul
              initial={{ opacity: 0, y: -6 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -6 }}
              className="absolute z-20 mt-1.5 w-full overflow-hidden rounded-xl border bg-popover shadow-float"
            >
              {suggestions.map((s) => (
                <li key={s.name} className="border-b last:border-0">
                  <div className="flex flex-wrap items-center gap-2 px-3 py-2.5">
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-semibold">{s.name}</p>
                      <p className="truncate text-[11px] text-muted-foreground">{s.class}</p>
                    </div>
                    <div className="flex shrink-0 gap-1">
                      {s.strengths.map((st) => (
                        <Button
                          key={st}
                          size="sm"
                          variant="outline"
                          className="h-7 rounded-lg text-[11px]"
                          onClick={() => addMed(s.name, st)}
                        >
                          <Plus className="size-3" />
                          {st}
                        </Button>
                      ))}
                    </div>
                  </div>
                </li>
              ))}
            </motion.ul>
          )}
        </AnimatePresence>
      </div>

      <div className="mt-4 space-y-3">
        <AnimatePresence initial={false}>
          {items.map((item, idx) => (
            <motion.div
              key={idx}
              layout
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, x: -12 }}
              className="rounded-xl border bg-card/70 p-3 space-y-2.5"
            >
              <div className="flex items-center justify-between gap-2">
                <div className="flex-1 min-w-0 flex items-center gap-2">
                  <Input
                    value={item.medicineName}
                    onChange={(e) => updateItem(idx, { medicineName: e.target.value })}
                    placeholder="Medicine Name *"
                    className="h-8 font-bold text-xs rounded-lg max-w-xs"
                  />
                  <Input
                    value={item.dosage}
                    onChange={(e) => updateItem(idx, { dosage: e.target.value })}
                    placeholder="Dosage (e.g. 500 mg) *"
                    className="h-8 text-xs rounded-lg max-w-32"
                  />
                </div>
                <Button
                  size="icon"
                  variant="ghost"
                  className="size-8 shrink-0 rounded-lg text-muted-foreground hover:text-destructive"
                  onClick={() => removeMed(idx)}
                >
                  <Trash2 className="size-3.5" />
                </Button>
              </div>

              {allergyRisk(item.medicineName) && (
                <p className="text-[11px] font-semibold text-destructive flex items-center gap-1">
                  <AlertCircle className="size-3" /> Allergy conflict flagged — verify before prescribing
                </p>
              )}

              <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs">
                <div>
                  <label className="text-[10px] text-muted-foreground font-semibold">Frequency *</label>
                  <Input
                    value={item.frequency}
                    onChange={(e) => updateItem(idx, { frequency: e.target.value })}
                    placeholder="e.g. Twice daily"
                    className="h-8 text-xs rounded-lg mt-0.5"
                  />
                </div>
                <div>
                  <label className="text-[10px] text-muted-foreground font-semibold">Duration *</label>
                  <Input
                    value={item.duration}
                    onChange={(e) => updateItem(idx, { duration: e.target.value })}
                    placeholder="e.g. 5 days"
                    className="h-8 text-xs rounded-lg mt-0.5"
                  />
                </div>
                <div>
                  <label className="text-[10px] text-muted-foreground font-semibold">Route</label>
                  <Input
                    value={item.route || "ORAL"}
                    onChange={(e) => updateItem(idx, { route: e.target.value })}
                    placeholder="e.g. ORAL"
                    className="h-8 text-xs rounded-lg mt-0.5"
                  />
                </div>
                <div>
                  <label className="text-[10px] text-muted-foreground font-semibold">Instructions</label>
                  <Input
                    value={item.instructions || ""}
                    onChange={(e) => updateItem(idx, { instructions: e.target.value })}
                    placeholder="e.g. After food"
                    className="h-8 text-xs rounded-lg mt-0.5"
                  />
                </div>
              </div>
            </motion.div>
          ))}
        </AnimatePresence>

        {items.length === 0 && (
          <p className="rounded-xl bg-muted/60 px-4 py-8 text-center text-sm text-muted-foreground">
            No medicines added yet. Search above or click 'Add Custom Medicine' to build prescription.
          </p>
        )}

        <Button
          type="button"
          variant="outline"
          size="sm"
          className="rounded-xl text-xs"
          onClick={() =>
            setItems((prev) => [
              ...prev,
              { medicineName: "", dosage: "", frequency: "Twice daily", duration: "5 days", route: "ORAL", instructions: "" },
            ])
          }
        >
          <Plus className="size-3.5 mr-1" /> Add Custom Medicine
        </Button>
      </div>

      <div className="mt-4 space-y-1.5">
        <label className="text-xs font-semibold text-muted-foreground">Prescription Notes / Advice</label>
        <Input
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          placeholder="Special clinical advice or dietary instructions..."
          className="rounded-xl text-xs h-9"
        />
      </div>

      <div className="mt-5 flex flex-wrap items-center gap-2 pt-3 border-t">
        <Button
          type="button"
          variant="outline"
          disabled={saving || issuing || cancelling}
          className="rounded-xl"
          onClick={handleSaveDraft}
        >
          {saving ? <Loader2 className="size-3.5 animate-spin mr-1" /> : <Save className="size-3.5 mr-1" />}
          Save Draft
        </Button>

        <Button
          type="button"
          variant="default"
          disabled={saving || issuing || cancelling}
          className="rounded-xl"
          onClick={() => setShowIssueConfirm(true)}
        >
          {issuing ? <Loader2 className="size-3.5 animate-spin mr-1" /> : <Send className="size-3.5 mr-1" />}
          Issue Prescription
        </Button>

        {prescription && (
          <Button
            type="button"
            variant="destructive"
            disabled={saving || issuing || cancelling}
            className="rounded-xl"
            onClick={() => setShowCancelConfirm(true)}
          >
            {cancelling ? <Loader2 className="size-3.5 animate-spin mr-1" /> : <X className="size-3.5 mr-1" />}
            Cancel Prescription
          </Button>
        )}
      </div>

      {/* Confirmation Dialogs */}
      <AlertDialog open={showIssueConfirm} onOpenChange={setShowIssueConfirm}>
        <AlertDialogContent className="rounded-2xl">
          <AlertDialogHeader>
            <AlertDialogTitle>Issue Prescription?</AlertDialogTitle>
            <AlertDialogDescription>
              Are you sure you want to issue this prescription? Once issued, it becomes an official clinical record and cannot be edited.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel className="rounded-xl">Back</AlertDialogCancel>
            <AlertDialogAction className="rounded-xl" onClick={handleIssueConfirm}>
              Yes, Issue Prescription
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <AlertDialog open={showCancelConfirm} onOpenChange={setShowCancelConfirm}>
        <AlertDialogContent className="rounded-2xl">
          <AlertDialogHeader>
            <AlertDialogTitle>Cancel Prescription?</AlertDialogTitle>
            <AlertDialogDescription>
              Are you sure you want to cancel this prescription draft? This action cannot be undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel className="rounded-xl">Back</AlertDialogCancel>
            <AlertDialogAction className="rounded-xl bg-destructive text-destructive-foreground hover:bg-destructive/90" onClick={handleCancelConfirm}>
              Yes, Cancel
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </section>
  );
}
