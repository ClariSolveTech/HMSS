import { createFileRoute, Link, useParams } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import {
  Pill,
  CalendarDays,
  User,
  ArrowLeft,
  Clock,
  Loader2,
  AlertCircle,
  FileText,
  CheckCircle2,
  Info,
} from "lucide-react";

import { PatientAppShell } from "@/components/patient/patient-app-shell";
import { UnlinkedAccountCard } from "@/components/patient/unlinked-account-card";
import {
  patientPortalApi,
  getPortalErrorMessage,
  isUnlinkedProfileError,
} from "@/services/patient-portal-api";
import { isAuthenticated } from "@/lib/auth";
import { Button } from "@/components/ui/button";

export const Route = createFileRoute("/patient/prescriptions/$prescriptionId")({
  head: () => ({
    meta: [
      { title: "Prescription Details — Patient Portal" },
      {
        name: "description",
        content: "Prescribed medicines, dosage instructions, and directions for use.",
      },
    ],
  }),
  component: PatientPrescriptionDetailPage,
});

function PatientPrescriptionDetailPage() {
  const { prescriptionId } = useParams({ from: "/patient/prescriptions/$prescriptionId" });
  const idNum = Number(prescriptionId);

  const {
    data: prescription,
    isLoading,
    error,
    refetch,
  } = useQuery({
    queryKey: ["patient-prescription-detail", idNum],
    queryFn: () => patientPortalApi.getPrescriptionDetail(idNum),
    enabled: !isNaN(idNum) && isAuthenticated(),
  });

  const isUnlinked = isUnlinkedProfileError(error);

  if (isUnlinked) {
    return (
      <PatientAppShell>
        <UnlinkedAccountCard
          message={getPortalErrorMessage(error, "No patient clinical profile linked.")}
        />
      </PatientAppShell>
    );
  }

  return (
    <PatientAppShell>
      <div className="space-y-6 max-w-3xl mx-auto">
        {/* Back Link */}
        <Link
          to="/patient/prescriptions"
          className="inline-flex items-center gap-1.5 text-sm font-bold text-slate-600 hover:text-slate-900 transition-colors"
        >
          <ArrowLeft className="size-4" /> Back to Prescriptions
        </Link>

        {/* Loading */}
        {isLoading && (
          <div className="p-12 text-center bg-white rounded-3xl border border-slate-200">
            <Loader2 className="size-8 animate-spin text-emerald-600 mx-auto mb-3" />
            <p className="text-slate-600 font-semibold">Loading medication details...</p>
          </div>
        )}

        {/* Error */}
        {error && (
          <div className="p-6 bg-rose-50 border border-rose-200 rounded-3xl text-rose-800 flex items-start gap-3">
            <AlertCircle className="size-6 shrink-0 mt-0.5 text-rose-600" />
            <div className="space-y-1">
              <p className="font-bold">Prescription Not Found</p>
              <p className="text-sm text-rose-700">
                {getPortalErrorMessage(error, "This prescription record could not be found.")}
              </p>
              <Button
                variant="outline"
                size="sm"
                onClick={() => refetch()}
                className="mt-2 text-rose-900 border-rose-300 hover:bg-rose-100 rounded-xl"
              >
                Retry
              </Button>
            </div>
          </div>
        )}

        {prescription && (
          <div className="space-y-6">
            {/* Header Card */}
            <div className="bg-white p-6 sm:p-8 rounded-3xl border border-slate-200 shadow-xs space-y-4">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <span className="text-sm font-bold text-slate-700 bg-slate-100 px-3 py-1 rounded-xl">
                  Rx #{prescription.prescriptionCode}
                </span>
                <span className="text-xs font-bold text-emerald-800 bg-emerald-100 px-3 py-1 rounded-xl flex items-center gap-1">
                  <CheckCircle2 className="size-4 text-emerald-700" /> {prescription.status}
                </span>
              </div>

              <div>
                <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900">
                  Prescribed by {prescription.doctorName}
                </h1>
                <p className="text-sm font-semibold text-slate-500 flex items-center gap-1.5 mt-1">
                  <CalendarDays className="size-4 text-emerald-600" /> Issued on{" "}
                  {prescription.issuedDate}
                </p>
              </div>

              {prescription.notes && (
                <div className="p-4 bg-emerald-50/70 border border-emerald-200/80 rounded-2xl text-emerald-950 text-sm">
                  <span className="font-bold block mb-1">Doctor's Clinical Notes:</span>
                  {prescription.notes}
                </div>
              )}
            </div>

            {/* Medicines List */}
            <div className="space-y-4">
              <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
                <Pill className="size-5 text-emerald-600" /> Prescribed Medications (
                {prescription.items?.length || 0})
              </h2>

              {!prescription.items || prescription.items.length === 0 ? (
                <div className="p-6 bg-white rounded-2xl border border-slate-200 text-center text-slate-500 text-sm">
                  No medicine items recorded on this prescription.
                </div>
              ) : (
                prescription.items.map((item, index) => (
                  <div
                    key={item.id || index}
                    className="bg-white p-5 sm:p-6 rounded-3xl border border-slate-200 shadow-xs space-y-3"
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <span className="text-xs font-bold text-emerald-700 uppercase tracking-wider block">
                          Medicine #{index + 1}
                        </span>
                        <h3 className="text-xl font-extrabold text-slate-900 mt-0.5">
                          {item.medicineName}
                        </h3>
                      </div>
                      <span className="text-sm font-bold bg-slate-100 text-slate-800 px-3 py-1 rounded-xl">
                        {item.dosage}
                      </span>
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-2 text-sm">
                      <div className="bg-slate-50 p-3 rounded-xl space-y-0.5">
                        <span className="text-xs font-bold text-slate-400 uppercase">
                          Frequency
                        </span>
                        <p className="font-bold text-slate-800">{item.frequency}</p>
                      </div>

                      <div className="bg-slate-50 p-3 rounded-xl space-y-0.5">
                        <span className="text-xs font-bold text-slate-400 uppercase">Duration</span>
                        <p className="font-bold text-slate-800">{item.duration}</p>
                      </div>

                      <div className="bg-slate-50 p-3 rounded-xl space-y-0.5">
                        <span className="text-xs font-bold text-slate-400 uppercase">Route</span>
                        <p className="font-bold text-slate-800">{item.route || "Oral"}</p>
                      </div>
                    </div>

                    {item.instructions && (
                      <div className="p-3 bg-amber-50/80 border border-amber-200/80 rounded-xl text-xs sm:text-sm text-amber-900 flex items-start gap-2">
                        <Info className="size-4 shrink-0 mt-0.5 text-amber-600" />
                        <div>
                          <span className="font-bold">Instructions: </span>
                          {item.instructions}
                        </div>
                      </div>
                    )}
                  </div>
                ))
              )}
            </div>

            <div className="p-4 bg-slate-100 rounded-2xl text-xs text-slate-600">
              <span className="font-bold">Important Patient Note: </span>
              Always take medications as prescribed by your physician. If you experience adverse
              side effects, contact your doctor or hospital immediately.
            </div>
          </div>
        )}
      </div>
    </PatientAppShell>
  );
}
