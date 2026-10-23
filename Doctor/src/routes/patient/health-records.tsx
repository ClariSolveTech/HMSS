import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  FileText,
  Stethoscope,
  CalendarDays,
  User,
  FlaskConical,
  CheckCircle2,
  Clock,
  Loader2,
  AlertCircle,
  Activity,
} from "lucide-react";

import { PatientAppShell } from "@/components/patient/patient-app-shell";
import { UnlinkedAccountCard } from "@/components/patient/unlinked-account-card";
import {
  patientPortalApi,
  getPortalErrorMessage,
  isUnlinkedProfileError,
  type PatientConsultationSummaryResponse,
  type PatientLabOrderSummaryResponse,
} from "@/services/patient-portal-api";
import { isAuthenticated } from "@/lib/auth";
import { Button } from "@/components/ui/button";

export const Route = createFileRoute("/patient/health-records")({
  head: () => ({
    meta: [
      { title: "Health Records & Lab Tests — Patient Portal" },
      {
        name: "description",
        content: "Access your clinical consultation notes and laboratory test orders.",
      },
    ],
  }),
  component: PatientHealthRecordsPage,
});

function PatientHealthRecordsPage() {
  const [activeTab, setActiveTab] = useState<"CONSULTATIONS" | "LAB_ORDERS">("CONSULTATIONS");

  // Consultations query
  const {
    data: consultationsData,
    isLoading: loadingConsultations,
    error: errorConsultations,
    refetch: refetchConsultations,
  } = useQuery({
    queryKey: ["patient-consultations"],
    queryFn: () => patientPortalApi.getConsultations({ page: 0, size: 20 }),
    enabled: activeTab === "CONSULTATIONS" && isAuthenticated(),
  });

  // Lab Orders query
  const {
    data: labOrdersData,
    isLoading: loadingLabOrders,
    error: errorLabOrders,
    refetch: refetchLabOrders,
  } = useQuery({
    queryKey: ["patient-lab-orders"],
    queryFn: () => patientPortalApi.getLabOrders({ page: 0, size: 20 }),
    enabled: activeTab === "LAB_ORDERS" && isAuthenticated(),
  });

  const activeError = activeTab === "CONSULTATIONS" ? errorConsultations : errorLabOrders;
  const isUnlinked = isUnlinkedProfileError(activeError);

  if (isUnlinked) {
    return (
      <PatientAppShell>
        <UnlinkedAccountCard
          message={getPortalErrorMessage(activeError, "No patient clinical profile linked.")}
        />
      </PatientAppShell>
    );
  }

  const consultations: PatientConsultationSummaryResponse[] = consultationsData?.content || [];
  const labOrders: PatientLabOrderSummaryResponse[] = labOrdersData?.content || [];

  return (
    <PatientAppShell>
      <div className="space-y-6 max-w-4xl mx-auto">
        {/* Header */}
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
            My Health Records
          </h1>
          <p className="mt-1 text-sm text-slate-600">
            Review past physician consultation summaries and diagnostic laboratory orders.
          </p>
        </div>

        {/* Tab Toggle (>= 44px touch targets) */}
        <div className="flex gap-2 p-1.5 bg-slate-200/70 rounded-2xl">
          <button
            onClick={() => setActiveTab("CONSULTATIONS")}
            className={`flex-1 min-h-[46px] px-4 py-2 rounded-xl text-sm font-bold flex items-center justify-center gap-2 transition-all ${
              activeTab === "CONSULTATIONS"
                ? "bg-white text-emerald-800 shadow-xs"
                : "text-slate-600 hover:text-slate-900 hover:bg-slate-200"
            }`}
          >
            <Stethoscope className="size-4" /> Doctor Consultations
          </button>
          <button
            onClick={() => setActiveTab("LAB_ORDERS")}
            className={`flex-1 min-h-[46px] px-4 py-2 rounded-xl text-sm font-bold flex items-center justify-center gap-2 transition-all ${
              activeTab === "LAB_ORDERS"
                ? "bg-white text-emerald-800 shadow-xs"
                : "text-slate-600 hover:text-slate-900 hover:bg-slate-200"
            }`}
          >
            <FlaskConical className="size-4" /> Lab Test Orders
          </button>
        </div>

        {/* Tab 1: Consultations */}
        {activeTab === "CONSULTATIONS" && (
          <div className="space-y-4">
            {loadingConsultations && (
              <div className="p-12 text-center bg-white rounded-3xl border border-slate-200">
                <Loader2 className="size-8 animate-spin text-emerald-600 mx-auto mb-3" />
                <p className="text-slate-600 font-semibold">Loading consultation records...</p>
              </div>
            )}

            {errorConsultations && (
              <div className="p-6 bg-rose-50 border border-rose-200 rounded-3xl text-rose-800 flex items-start gap-3">
                <AlertCircle className="size-6 shrink-0 mt-0.5 text-rose-600" />
                <div className="space-y-1">
                  <p className="font-bold">Error loading consultations</p>
                  <p className="text-sm text-rose-700">
                    {getPortalErrorMessage(
                      errorConsultations,
                      "Failed to load clinical consultations.",
                    )}
                  </p>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => refetchConsultations()}
                    className="mt-2 text-rose-900 border-rose-300 hover:bg-rose-100 rounded-xl"
                  >
                    Try Again
                  </Button>
                </div>
              </div>
            )}

            {!loadingConsultations && !errorConsultations && consultations.length === 0 && (
              <div className="p-10 text-center bg-white rounded-3xl border border-slate-200 space-y-3">
                <div className="size-14 mx-auto rounded-full bg-slate-100 flex items-center justify-center text-slate-400">
                  <FileText className="size-7" />
                </div>
                <h3 className="text-lg font-bold text-slate-800">No Consultation Records Found</h3>
                <p className="text-sm text-slate-500 max-w-sm mx-auto">
                  Completed doctor consultations and clinical summaries will appear here once
                  finalized by your physician.
                </p>
              </div>
            )}

            {!loadingConsultations && !errorConsultations && consultations.length > 0 && (
              <div className="space-y-4">
                {consultations.map((c) => (
                  <div
                    key={c.id}
                    className="bg-white p-5 sm:p-7 rounded-3xl border border-slate-200 shadow-xs space-y-4"
                  >
                    <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-100 pb-3">
                      <div>
                        <span className="text-xs font-bold text-emerald-700 uppercase tracking-wider block">
                          Doctor Consultation
                        </span>
                        <h2 className="text-xl font-extrabold text-slate-900">{c.doctorName}</h2>
                      </div>
                      <span className="text-xs font-semibold text-slate-600 bg-slate-100 px-3 py-1 rounded-xl flex items-center gap-1.5">
                        <CalendarDays className="size-3.5 text-emerald-600" /> {c.consultationDate}
                      </span>
                    </div>

                    <div className="space-y-3 text-sm">
                      {c.chiefComplaint && (
                        <div>
                          <span className="text-xs font-bold uppercase text-slate-500 block">
                            Chief Symptoms / Complaint
                          </span>
                          <p className="text-slate-800 font-medium mt-0.5">{c.chiefComplaint}</p>
                        </div>
                      )}

                      {c.diagnosis && (
                        <div className="p-3 bg-emerald-50/70 border border-emerald-200/70 rounded-2xl">
                          <span className="text-xs font-bold uppercase text-emerald-800 block">
                            Clinical Diagnosis
                          </span>
                          <p className="text-emerald-950 font-bold mt-0.5">{c.diagnosis}</p>
                        </div>
                      )}

                      {c.treatmentAdvice && (
                        <div className="p-3 bg-slate-50 rounded-2xl">
                          <span className="text-xs font-bold uppercase text-slate-500 block">
                            Treatment Advice & Instructions
                          </span>
                          <p className="text-slate-800 font-medium mt-0.5 leading-relaxed">
                            {c.treatmentAdvice}
                          </p>
                        </div>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Tab 2: Lab Orders */}
        {activeTab === "LAB_ORDERS" && (
          <div className="space-y-4">
            {loadingLabOrders && (
              <div className="p-12 text-center bg-white rounded-3xl border border-slate-200">
                <Loader2 className="size-8 animate-spin text-emerald-600 mx-auto mb-3" />
                <p className="text-slate-600 font-semibold">Loading lab test orders...</p>
              </div>
            )}

            {errorLabOrders && (
              <div className="p-6 bg-rose-50 border border-rose-200 rounded-3xl text-rose-800 flex items-start gap-3">
                <AlertCircle className="size-6 shrink-0 mt-0.5 text-rose-600" />
                <div className="space-y-1">
                  <p className="font-bold">Error loading lab orders</p>
                  <p className="text-sm text-rose-700">
                    {getPortalErrorMessage(errorLabOrders, "Failed to load diagnostic orders.")}
                  </p>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => refetchLabOrders()}
                    className="mt-2 text-rose-900 border-rose-300 hover:bg-rose-100 rounded-xl"
                  >
                    Try Again
                  </Button>
                </div>
              </div>
            )}

            {!loadingLabOrders && !errorLabOrders && labOrders.length === 0 && (
              <div className="p-10 text-center bg-white rounded-3xl border border-slate-200 space-y-3">
                <div className="size-14 mx-auto rounded-full bg-slate-100 flex items-center justify-center text-slate-400">
                  <FlaskConical className="size-7" />
                </div>
                <h3 className="text-lg font-bold text-slate-800">No Lab Orders Recorded</h3>
                <p className="text-sm text-slate-500 max-w-sm mx-auto">
                  Diagnostic lab orders issued by your doctor will be listed here along with their
                  processing status.
                </p>
              </div>
            )}

            {!loadingLabOrders && !errorLabOrders && labOrders.length > 0 && (
              <div className="space-y-4">
                {labOrders.map((order) => (
                  <div
                    key={order.id}
                    className="bg-white p-5 sm:p-6 rounded-3xl border border-slate-200 shadow-xs space-y-3"
                  >
                    <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-100 pb-2">
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-bold text-slate-700 bg-slate-100 px-2.5 py-1 rounded-lg">
                          #{order.orderNumber}
                        </span>
                        <span className="text-xs font-bold text-emerald-800 bg-emerald-100 px-2.5 py-1 rounded-lg flex items-center gap-1">
                          <CheckCircle2 className="size-3.5 text-emerald-700" /> {order.status}
                        </span>
                      </div>
                      <span className="text-xs font-semibold text-slate-500">
                        Ordered: {order.orderedAt ? order.orderedAt.split("T")[0] : "Recently"}
                      </span>
                    </div>

                    <div>
                      <span className="text-xs text-slate-500 font-semibold block">
                        Ordering Physician: {order.doctorName}
                      </span>
                    </div>

                    <div>
                      <span className="text-xs font-bold uppercase text-slate-500 block mb-1">
                        Tests Included in Order:
                      </span>
                      <div className="flex flex-wrap gap-2">
                        {order.testNames && order.testNames.length > 0 ? (
                          order.testNames.map((name, i) => (
                            <span
                              key={i}
                              className="px-3 py-1 bg-slate-100 text-slate-800 rounded-xl text-xs font-bold border border-slate-200"
                            >
                              {name}
                            </span>
                          ))
                        ) : (
                          <span className="text-xs text-slate-500">Diagnostic investigation</span>
                        )}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    </PatientAppShell>
  );
}
