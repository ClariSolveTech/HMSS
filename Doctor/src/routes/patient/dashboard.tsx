import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import {
  CalendarDays,
  Pill,
  FileText,
  Video,
  Clock,
  User,
  ArrowRight,
  ExternalLink,
  AlertCircle,
  Loader2,
  CalendarCheck2,
  Stethoscope,
  Sparkles,
} from "lucide-react";

import { PatientAppShell } from "@/components/patient/patient-app-shell";
import { UnlinkedAccountCard } from "@/components/patient/unlinked-account-card";
import {
  patientPortalApi,
  getPortalErrorMessage,
  isUnlinkedProfileError,
  type PatientAppointmentSummary,
} from "@/services/patient-portal-api";
import { isAuthenticated } from "@/lib/auth";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";

export const Route = createFileRoute("/patient/dashboard")({
  head: () => ({
    meta: [
      { title: "My Health Dashboard — Patient Portal" },
      {
        name: "description",
        content: "Overview of your upcoming appointments, prescriptions, and medical records.",
      },
    ],
  }),
  component: PatientDashboardPage,
});

function PatientDashboardPage() {
  const navigate = useNavigate();

  const {
    data: summary,
    isLoading,
    error,
    refetch,
  } = useQuery({
    queryKey: ["patient-dashboard-summary"],
    queryFn: () => patientPortalApi.getDashboard(),
    enabled: isAuthenticated(),
    retry: 1,
  });

  // Handle unlinked clinical account
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
      <div className="space-y-6 max-w-5xl mx-auto">
        {/* Welcome Banner */}
        <div className="bg-gradient-to-r from-emerald-600 via-teal-600 to-cyan-700 text-white p-6 sm:p-8 rounded-3xl shadow-md relative overflow-hidden">
          <div className="relative z-10 space-y-2">
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-white/20 text-white backdrop-blur-xs">
              <Sparkles className="size-3.5" /> Patient Health Workspace
            </span>
            <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight">
              {isLoading
                ? "Loading your dashboard..."
                : summary?.patientName
                  ? `Hello, ${summary.patientName}!`
                  : "Welcome to Your Patient Portal"}
            </h1>
            <p className="text-sm sm:text-base text-emerald-100 max-w-2xl font-medium">
              {summary?.hospitalName
                ? `Hospital: ${summary.hospitalName} · Patient ID: ${summary.patientCode}`
                : "Manage your hospital visits, consultations, and prescribed medicines in one place."}
            </p>
          </div>
        </div>

        {/* Loading State */}
        {isLoading && (
          <div className="p-12 text-center bg-white rounded-3xl border border-slate-200">
            <Loader2 className="size-8 animate-spin text-emerald-600 mx-auto mb-3" />
            <p className="text-slate-600 font-semibold">Loading your health summary...</p>
          </div>
        )}

        {/* Error State */}
        {error && !isUnlinked && (
          <div className="p-6 bg-rose-50 border border-rose-200 rounded-3xl text-rose-800 flex items-start gap-3">
            <AlertCircle className="size-6 shrink-0 mt-0.5 text-rose-600" />
            <div className="space-y-1">
              <p className="font-bold">Unable to load dashboard</p>
              <p className="text-sm text-rose-700">
                {getPortalErrorMessage(
                  error,
                  "A network or server error occurred. Please try again.",
                )}
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

        {/* Content Loaded */}
        {summary && (
          <>
            {/* Next Appointment Card */}
            <div className="bg-white p-6 sm:p-7 rounded-3xl border border-slate-200/90 shadow-xs">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
                  <CalendarCheck2 className="size-5 text-emerald-600" /> Next Upcoming Appointment
                </h2>
                <Link
                  to="/patient/appointments"
                  className="text-sm font-bold text-emerald-700 hover:text-emerald-800 hover:underline flex items-center gap-1"
                >
                  View all <ArrowRight className="size-4" />
                </Link>
              </div>

              {summary.nextAppointment ? (
                <div className="p-5 sm:p-6 bg-emerald-50/60 border border-emerald-200/70 rounded-2xl flex flex-col md:flex-row md:items-center justify-between gap-4">
                  <div className="space-y-2">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="text-xs font-bold text-emerald-800 bg-emerald-200/70 px-2.5 py-0.5 rounded-lg">
                        {summary.nextAppointment.appointmentNumber}
                      </span>
                      {summary.nextAppointment.isTeleconsultation ? (
                        <span className="text-xs font-bold text-indigo-800 bg-indigo-100 px-2.5 py-0.5 rounded-lg flex items-center gap-1">
                          <Video className="size-3.5" /> Video Teleconsultation
                        </span>
                      ) : (
                        <span className="text-xs font-bold text-slate-700 bg-slate-200 px-2.5 py-0.5 rounded-lg">
                          In-Person Visit
                        </span>
                      )}
                      <span className="text-xs font-bold text-emerald-700 uppercase">
                        {summary.nextAppointment.status}
                      </span>
                    </div>

                    <h3 className="text-xl font-extrabold text-slate-900">
                      {summary.nextAppointment.doctorName}
                    </h3>
                    <p className="text-sm text-slate-600 font-medium">
                      Specialty: {summary.nextAppointment.specialization || "General Medicine"}
                    </p>

                    <div className="flex flex-wrap items-center gap-4 text-sm font-semibold text-slate-700 pt-1">
                      <span className="flex items-center gap-1.5 text-slate-800">
                        <CalendarDays className="size-4 text-emerald-600" />
                        {summary.nextAppointment.appointmentDate}
                      </span>
                      <span className="flex items-center gap-1.5 text-slate-800">
                        <Clock className="size-4 text-emerald-600" />
                        {summary.nextAppointment.startTime} - {summary.nextAppointment.endTime}
                      </span>
                    </div>
                  </div>

                  {/* Teleconsult Join Action or Details Button */}
                  <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3 shrink-0">
                    {summary.nextAppointment.isTeleconsultation && (
                      <Link
                        to="/patient/appointments/$appointmentId"
                        params={{ appointmentId: String(summary.nextAppointment.id) }}
                      >
                        <Button
                          className={`min-h-[46px] w-full px-5 rounded-xl font-bold shadow-xs ${
                            summary.nextAppointment.canJoinConsultation
                              ? "bg-emerald-600 hover:bg-emerald-700 text-white animate-pulse"
                              : "bg-indigo-600 hover:bg-indigo-700 text-white"
                          }`}
                        >
                          <Video className="size-4 mr-2" />
                          {summary.nextAppointment.canJoinConsultation
                            ? "Join Video Room Now"
                            : "Video Room Status"}
                        </Button>
                      </Link>
                    )}

                    <Link
                      to="/patient/appointments/$appointmentId"
                      params={{ appointmentId: String(summary.nextAppointment.id) }}
                    >
                      <Button
                        variant="outline"
                        className="min-h-[46px] w-full px-4 rounded-xl font-bold border-slate-300 hover:bg-slate-100"
                      >
                        View Appointment
                      </Button>
                    </Link>
                  </div>
                </div>
              ) : (
                <div className="p-6 bg-slate-50 border border-slate-200/80 rounded-2xl text-center space-y-2">
                  <p className="text-base font-bold text-slate-800">
                    No Upcoming Appointments Scheduled
                  </p>
                  <p className="text-sm text-slate-500 max-w-md mx-auto">
                    You currently have no scheduled appointments. Contact your hospital reception to
                    schedule your next visit or check-up.
                  </p>
                </div>
              )}
            </div>

            {/* Quick Metrics / Action Cards */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
              {/* Prescriptions */}
              <Link
                to="/patient/prescriptions"
                className="bg-white p-6 rounded-3xl border border-slate-200 shadow-xs hover:shadow-md hover:border-emerald-300 transition-all group flex flex-col justify-between"
              >
                <div className="space-y-3">
                  <div className="size-12 rounded-2xl bg-teal-100 text-teal-800 flex items-center justify-center group-hover:scale-105 transition-transform">
                    <Pill className="size-6" />
                  </div>
                  <div>
                    <h3 className="text-lg font-bold text-slate-900">My Prescriptions</h3>
                    <p className="text-sm text-slate-500">
                      View your current medicines, dosage instructions, and refill records.
                    </p>
                  </div>
                </div>
                <div className="mt-5 pt-3 border-t border-slate-100 flex items-center justify-between">
                  <span className="text-2xl font-black text-slate-900">
                    {summary.activePrescriptionsCount}
                  </span>
                  <span className="text-sm font-bold text-emerald-700 flex items-center gap-1 group-hover:translate-x-1 transition-transform">
                    View medicines &rarr;
                  </span>
                </div>
              </Link>

              {/* Consultations */}
              <Link
                to="/patient/health-records"
                className="bg-white p-6 rounded-3xl border border-slate-200 shadow-xs hover:shadow-md hover:border-emerald-300 transition-all group flex flex-col justify-between"
              >
                <div className="space-y-3">
                  <div className="size-12 rounded-2xl bg-indigo-100 text-indigo-800 flex items-center justify-center group-hover:scale-105 transition-transform">
                    <FileText className="size-6" />
                  </div>
                  <div>
                    <h3 className="text-lg font-bold text-slate-900">Health Records</h3>
                    <p className="text-sm text-slate-500">
                      Past doctor consultations, clinical notes, and physician advice.
                    </p>
                  </div>
                </div>
                <div className="mt-5 pt-3 border-t border-slate-100 flex items-center justify-between">
                  <span className="text-2xl font-black text-slate-900">
                    {summary.completedConsultationsCount}
                  </span>
                  <span className="text-sm font-bold text-indigo-700 flex items-center gap-1 group-hover:translate-x-1 transition-transform">
                    View records &rarr;
                  </span>
                </div>
              </Link>

              {/* Lab Orders */}
              <Link
                to="/patient/health-records"
                className="bg-white p-6 rounded-3xl border border-slate-200 shadow-xs hover:shadow-md hover:border-emerald-300 transition-all group flex flex-col justify-between"
              >
                <div className="space-y-3">
                  <div className="size-12 rounded-2xl bg-amber-100 text-amber-800 flex items-center justify-center group-hover:scale-105 transition-transform">
                    <Stethoscope className="size-6" />
                  </div>
                  <div>
                    <h3 className="text-lg font-bold text-slate-900">Lab Test Orders</h3>
                    <p className="text-sm text-slate-500">
                      Blood tests, diagnostics, and test completion tracking.
                    </p>
                  </div>
                </div>
                <div className="mt-5 pt-3 border-t border-slate-100 flex items-center justify-between">
                  <span className="text-2xl font-black text-slate-900">
                    {summary.labOrdersCount}
                  </span>
                  <span className="text-sm font-bold text-amber-800 flex items-center gap-1 group-hover:translate-x-1 transition-transform">
                    View tests &rarr;
                  </span>
                </div>
              </Link>
            </div>

            {/* Recent Appointments */}
            {summary.recentAppointments && summary.recentAppointments.length > 0 && (
              <div className="bg-white p-6 sm:p-7 rounded-3xl border border-slate-200 shadow-xs space-y-4">
                <div className="flex items-center justify-between">
                  <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
                    <Clock className="size-5 text-slate-600" /> Recent Appointments
                  </h2>
                  <Link
                    to="/patient/appointments"
                    className="text-sm font-bold text-emerald-700 hover:text-emerald-800 hover:underline"
                  >
                    View history &rarr;
                  </Link>
                </div>

                <div className="divide-y divide-slate-100">
                  {summary.recentAppointments.map((apt) => (
                    <div
                      key={apt.id}
                      className="py-3.5 flex flex-col sm:flex-row sm:items-center justify-between gap-3"
                    >
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-bold text-slate-900 text-base">
                            {apt.doctorName}
                          </span>
                          <span className="text-xs font-semibold text-slate-500 bg-slate-100 px-2 py-0.5 rounded-md">
                            {apt.appointmentDate}
                          </span>
                        </div>
                        <p className="text-xs text-slate-600 font-medium">
                          {apt.specialization || "General"} · {apt.startTime} - {apt.endTime}
                        </p>
                      </div>

                      <div className="flex items-center gap-2">
                        <span className="text-xs font-bold px-2.5 py-1 rounded-full uppercase bg-slate-100 text-slate-700">
                          {apt.status}
                        </span>
                        <Link
                          to="/patient/appointments/$appointmentId"
                          params={{ appointmentId: String(apt.id) }}
                        >
                          <Button
                            variant="ghost"
                            size="sm"
                            className="text-xs font-bold text-emerald-700 hover:bg-emerald-50 rounded-xl"
                          >
                            Details
                          </Button>
                        </Link>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </PatientAppShell>
  );
}
