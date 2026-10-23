import { createFileRoute, Link } from "@tanstack/react-router";
import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  CalendarDays,
  Clock,
  Video,
  CheckCircle2,
  XCircle,
  AlertCircle,
  Loader2,
  ArrowRight,
  Filter,
  User,
} from "lucide-react";

import { PatientAppShell } from "@/components/patient/patient-app-shell";
import { UnlinkedAccountCard } from "@/components/patient/unlinked-account-card";
import {
  patientPortalApi,
  getPortalErrorMessage,
  isUnlinkedProfileError,
  type AppointmentStatus,
  type AppointmentType,
  type PatientAppointmentSummary,
} from "@/services/patient-portal-api";
import { isAuthenticated } from "@/lib/auth";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";

export const Route = createFileRoute("/patient/appointments")({
  head: () => ({
    meta: [
      { title: "My Appointments — Patient Portal" },
      { name: "description", content: "View all your past and upcoming doctor consultations." },
    ],
  }),
  component: PatientAppointmentsPage,
});

function PatientAppointmentsPage() {
  const [filterType, setFilterType] = useState<"ALL" | "UPCOMING" | "COMPLETED" | "TELECONSULT">(
    "ALL",
  );
  const [page, setPage] = useState(0);

  let statusParam: AppointmentStatus | undefined;
  let typeParam: AppointmentType | undefined;

  if (filterType === "UPCOMING") statusParam = "CONFIRMED";
  if (filterType === "COMPLETED") statusParam = "COMPLETED";
  if (filterType === "TELECONSULT") typeParam = "TELECONSULTATION";

  const {
    data: pageData,
    isLoading,
    error,
    refetch,
  } = useQuery({
    queryKey: ["patient-appointments", filterType, page],
    queryFn: () =>
      patientPortalApi.getAppointments({
        status: statusParam,
        appointmentType: typeParam,
        page,
        size: 10,
        sort: "appointmentDate,desc",
      }),
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

  const appointments: PatientAppointmentSummary[] = pageData?.content || [];

  return (
    <PatientAppShell>
      <div className="space-y-6 max-w-4xl mx-auto">
        {/* Header */}
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
            My Appointments
          </h1>
          <p className="mt-1 text-sm text-slate-600">
            View your consultation schedule, doctor details, and video meeting status.
          </p>
        </div>

        {/* Filter Tabs (Large touch targets >= 44px) */}
        <div className="flex flex-wrap gap-2 p-1.5 bg-slate-200/70 rounded-2xl">
          {(
            [
              { id: "ALL", label: "All Consultations" },
              { id: "UPCOMING", label: "Upcoming (Confirmed)" },
              { id: "COMPLETED", label: "Completed" },
              { id: "TELECONSULT", label: "Video Consultations" },
            ] as const
          ).map((tab) => {
            const active = filterType === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => {
                  setFilterType(tab.id);
                  setPage(0);
                }}
                className={`flex-1 min-h-[44px] px-4 py-2 rounded-xl text-xs sm:text-sm font-bold transition-all ${
                  active
                    ? "bg-white text-emerald-800 shadow-xs"
                    : "text-slate-600 hover:text-slate-900 hover:bg-slate-200"
                }`}
              >
                {tab.label}
              </button>
            );
          })}
        </div>

        {/* Loading State */}
        {isLoading && (
          <div className="p-12 text-center bg-white rounded-3xl border border-slate-200">
            <Loader2 className="size-8 animate-spin text-emerald-600 mx-auto mb-3" />
            <p className="text-slate-600 font-semibold">Loading your appointments...</p>
          </div>
        )}

        {/* Error State */}
        {error && (
          <div className="p-6 bg-rose-50 border border-rose-200 rounded-3xl text-rose-800 flex items-start gap-3">
            <AlertCircle className="size-6 shrink-0 mt-0.5 text-rose-600" />
            <div className="space-y-1">
              <p className="font-bold">Error loading appointments</p>
              <p className="text-sm text-rose-700">
                {getPortalErrorMessage(error, "Failed to load consultations from the server.")}
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
        {!isLoading && !error && appointments.length === 0 && (
          <div className="p-10 text-center bg-white rounded-3xl border border-slate-200 space-y-3">
            <div className="size-14 mx-auto rounded-full bg-slate-100 flex items-center justify-center text-slate-400">
              <CalendarDays className="size-7" />
            </div>
            <h3 className="text-lg font-bold text-slate-800">No Appointments Found</h3>
            <p className="text-sm text-slate-500 max-w-sm mx-auto">
              There are no appointments matching the selected filter. Need to book a consultation?
              Please contact hospital reception.
            </p>
          </div>
        )}

        {/* Appointments List */}
        {!isLoading && !error && appointments.length > 0 && (
          <div className="space-y-4">
            {appointments.map((apt) => {
              const isTele = apt.isTeleconsultation;
              return (
                <div
                  key={apt.id}
                  className="bg-white p-5 sm:p-6 rounded-3xl border border-slate-200 shadow-xs hover:border-emerald-300 hover:shadow-md transition-all flex flex-col md:flex-row md:items-center justify-between gap-4"
                >
                  <div className="space-y-2">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="text-xs font-bold text-slate-700 bg-slate-100 px-2.5 py-1 rounded-lg">
                        #{apt.appointmentNumber}
                      </span>
                      {isTele ? (
                        <span className="text-xs font-bold text-indigo-800 bg-indigo-50 px-2.5 py-1 rounded-lg flex items-center gap-1 border border-indigo-200">
                          <Video className="size-3.5 text-indigo-600" /> Video Teleconsultation
                        </span>
                      ) : (
                        <span className="text-xs font-bold text-slate-700 bg-slate-100 px-2.5 py-1 rounded-lg border border-slate-200">
                          In-Person Visit
                        </span>
                      )}

                      {/* Status Badges */}
                      {apt.status === "CONFIRMED" && (
                        <span className="text-xs font-bold text-emerald-800 bg-emerald-100 px-2.5 py-1 rounded-lg flex items-center gap-1">
                          <CheckCircle2 className="size-3.5 text-emerald-700" /> Confirmed
                        </span>
                      )}
                      {apt.status === "COMPLETED" && (
                        <span className="text-xs font-bold text-slate-700 bg-slate-200 px-2.5 py-1 rounded-lg flex items-center gap-1">
                          <CheckCircle2 className="size-3.5 text-slate-600" /> Completed
                        </span>
                      )}
                      {apt.status === "CANCELLED" && (
                        <span className="text-xs font-bold text-rose-800 bg-rose-100 px-2.5 py-1 rounded-lg flex items-center gap-1">
                          <XCircle className="size-3.5 text-rose-600" /> Cancelled
                        </span>
                      )}
                      {apt.status === "IN_PROGRESS" && (
                        <span className="text-xs font-bold text-blue-800 bg-blue-100 px-2.5 py-1 rounded-lg flex items-center gap-1">
                          <Clock className="size-3.5 text-blue-600" /> In Progress
                        </span>
                      )}
                    </div>

                    <h2 className="text-xl font-extrabold text-slate-900">{apt.doctorName}</h2>
                    <p className="text-sm font-semibold text-slate-600">
                      Specialization: {apt.specialization || "General Medicine"}
                    </p>

                    <div className="flex flex-wrap items-center gap-4 text-sm font-semibold text-slate-700 pt-1">
                      <span className="flex items-center gap-1.5">
                        <CalendarDays className="size-4 text-emerald-600" />
                        {apt.appointmentDate}
                      </span>
                      <span className="flex items-center gap-1.5">
                        <Clock className="size-4 text-emerald-600" />
                        {apt.startTime} - {apt.endTime}
                      </span>
                    </div>

                    {apt.reason && (
                      <p className="text-xs text-slate-500 italic pt-1">Reason: {apt.reason}</p>
                    )}
                  </div>

                  {/* Actions */}
                  <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3 shrink-0 pt-2 md:pt-0">
                    <Link
                      to="/patient/appointments/$appointmentId"
                      params={{ appointmentId: String(apt.id) }}
                    >
                      <Button className="min-h-[46px] w-full px-5 rounded-xl font-bold bg-emerald-600 hover:bg-emerald-700 text-white shadow-xs">
                        View Details <ArrowRight className="size-4 ml-1.5" />
                      </Button>
                    </Link>
                  </div>
                </div>
              );
            })}

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
