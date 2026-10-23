import { createFileRoute, Link } from "@tanstack/react-router";
import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  Pill,
  CalendarDays,
  User,
  ArrowRight,
  Loader2,
  AlertCircle,
  FileText,
  CheckCircle2,
} from "lucide-react";

import { PatientAppShell } from "@/components/patient/patient-app-shell";
import { UnlinkedAccountCard } from "@/components/patient/unlinked-account-card";
import {
  patientPortalApi,
  getPortalErrorMessage,
  isUnlinkedProfileError,
  type PatientPrescriptionSummaryResponse,
} from "@/services/patient-portal-api";
import { isAuthenticated } from "@/lib/auth";
import { Button } from "@/components/ui/button";

export const Route = createFileRoute("/patient/prescriptions")({
  head: () => ({
    meta: [
      { title: "My Prescriptions — Patient Portal" },
      {
        name: "description",
        content: "Review your active medicines, doctor instructions, and dosage schedules.",
      },
    ],
  }),
  component: PatientPrescriptionsPage,
});

function PatientPrescriptionsPage() {
  const [page, setPage] = useState(0);

  const {
    data: pageData,
    isLoading,
    error,
    refetch,
  } = useQuery({
    queryKey: ["patient-prescriptions", page],
    queryFn: () => patientPortalApi.getPrescriptions({ page, size: 10 }),
    enabled: isAuthenticated(),
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

  const prescriptions: PatientPrescriptionSummaryResponse[] = pageData?.content || [];

  return (
    <PatientAppShell>
      <div className="space-y-6 max-w-4xl mx-auto">
        {/* Header */}
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
            My Prescriptions
          </h1>
          <p className="mt-1 text-sm text-slate-600">
            View medication orders written by your consulting physicians.
          </p>
        </div>

        {/* Loading */}
        {isLoading && (
          <div className="p-12 text-center bg-white rounded-3xl border border-slate-200">
            <Loader2 className="size-8 animate-spin text-emerald-600 mx-auto mb-3" />
            <p className="text-slate-600 font-semibold">Loading prescriptions...</p>
          </div>
        )}

        {/* Error */}
        {error && (
          <div className="p-6 bg-rose-50 border border-rose-200 rounded-3xl text-rose-800 flex items-start gap-3">
            <AlertCircle className="size-6 shrink-0 mt-0.5 text-rose-600" />
            <div className="space-y-1">
              <p className="font-bold">Error loading prescriptions</p>
              <p className="text-sm text-rose-700">
                {getPortalErrorMessage(error, "Failed to retrieve prescriptions from the server.")}
              </p>
              <Button
                variant="outline"
                size="sm"
                onClick={() => refetch()}
                className="mt-2 text-rose-900 border-rose-300 hover:bg-rose-100 rounded-xl"
              >
                Try Again
              </Button>
            </div>
          </div>
        )}

        {/* Empty State */}
        {!isLoading && !error && prescriptions.length === 0 && (
          <div className="p-10 text-center bg-white rounded-3xl border border-slate-200 space-y-3">
            <div className="size-14 mx-auto rounded-full bg-slate-100 flex items-center justify-center text-slate-400">
              <Pill className="size-7" />
            </div>
            <h3 className="text-lg font-bold text-slate-800">No Prescriptions on File</h3>
            <p className="text-sm text-slate-500 max-w-sm mx-auto">
              You do not have any prescriptions recorded yet. Following your next doctor
              consultation, issued medications will appear here.
            </p>
          </div>
        )}

        {/* Prescriptions List */}
        {!isLoading && !error && prescriptions.length > 0 && (
          <div className="space-y-4">
            {prescriptions.map((rx) => (
              <div
                key={rx.id}
                className="bg-white p-5 sm:p-6 rounded-3xl border border-slate-200 shadow-xs hover:border-emerald-300 hover:shadow-md transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-4"
              >
                <div className="space-y-2">
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-bold text-slate-700 bg-slate-100 px-2.5 py-1 rounded-lg">
                      #{rx.prescriptionCode}
                    </span>
                    <span className="text-xs font-bold text-emerald-800 bg-emerald-100 px-2.5 py-1 rounded-lg flex items-center gap-1">
                      <CheckCircle2 className="size-3.5 text-emerald-700" /> {rx.status}
                    </span>
                  </div>

                  <h2 className="text-xl font-extrabold text-slate-900">
                    Prescribed by {rx.doctorName}
                  </h2>

                  <div className="flex flex-wrap items-center gap-4 text-sm font-semibold text-slate-600">
                    <span className="flex items-center gap-1.5">
                      <CalendarDays className="size-4 text-emerald-600" />
                      Issued: {rx.issuedDate}
                    </span>
                    <span className="flex items-center gap-1.5">
                      <Pill className="size-4 text-emerald-600" />
                      {rx.itemCount} {rx.itemCount === 1 ? "Medicine" : "Medicines"} Prescribed
                    </span>
                  </div>

                  {rx.notes && (
                    <p className="text-xs text-slate-500 italic pt-1">Notes: {rx.notes}</p>
                  )}
                </div>

                <div className="shrink-0 pt-2 sm:pt-0">
                  <Link
                    to="/patient/prescriptions/$prescriptionId"
                    params={{ prescriptionId: String(rx.id) }}
                  >
                    <Button className="min-h-[46px] w-full sm:w-auto px-5 rounded-xl font-bold bg-emerald-600 hover:bg-emerald-700 text-white shadow-xs">
                      View Medicines <ArrowRight className="size-4 ml-1.5" />
                    </Button>
                  </Link>
                </div>
              </div>
            ))}

            {/* Pagination Controls */}
            {pageData && pageData.totalPages > 1 && (
              <div className="flex items-center justify-between pt-4">
                <Button
                  variant="outline"
                  disabled={pageData.first}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  className="min-h-[44px] rounded-xl font-bold"
                >
                  &larr; Previous Page
                </Button>
                <span className="text-xs sm:text-sm font-bold text-slate-600">
                  Page {pageData.number + 1} of {pageData.totalPages}
                </span>
                <Button
                  variant="outline"
                  disabled={pageData.last}
                  onClick={() => setPage((p) => p + 1)}
                  className="min-h-[44px] rounded-xl font-bold"
                >
                  Next Page &rarr;
                </Button>
              </div>
            )}
          </div>
        )}
      </div>
    </PatientAppShell>
  );
}
