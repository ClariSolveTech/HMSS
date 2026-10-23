import { createFileRoute, Link, useParams } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import {
  CalendarDays,
  Clock,
  Video,
  Building2,
  User,
  ArrowLeft,
  ExternalLink,
  CheckCircle2,
  XCircle,
  AlertCircle,
  Loader2,
  ShieldAlert,
} from "lucide-react";

import { PatientAppShell } from "@/components/patient/patient-app-shell";
import { UnlinkedAccountCard } from "@/components/patient/unlinked-account-card";
import {
  patientPortalApi,
  getPortalErrorMessage,
  isUnlinkedProfileError,
  type PatientAppointmentDetailResponse,
  type TeleconsultAccessResponse,
} from "@/services/patient-portal-api";
import { isAuthenticated } from "@/lib/auth";
import { Button } from "@/components/ui/button";

export const Route = createFileRoute("/patient/appointments/$appointmentId")({
  head: () => ({
    meta: [
      { title: "Appointment Details — Patient Portal" },
      {
        name: "description",
        content: "Detailed appointment information and video consultation room access.",
      },
    ],
  }),
  component: PatientAppointmentDetailPage,
});

function PatientAppointmentDetailPage() {
  const { appointmentId } = useParams({ from: "/patient/appointments/$appointmentId" });
  const idNum = Number(appointmentId);

  // 1. Fetch appointment details
  const {
    data: appointment,
    isLoading: loadingApt,
    error: errorApt,
    refetch: refetchApt,
  } = useQuery({
    queryKey: ["patient-appointment-detail", idNum],
    queryFn: () => patientPortalApi.getAppointmentDetail(idNum),
    enabled: !isNaN(idNum) && isAuthenticated(),
  });

  // 2. Fetch teleconsult access only if teleconsultation and not in a terminal state
  const isTele = appointment?.isTeleconsultation;
  const isTerminal =
    appointment?.status === "CANCELLED" ||
    appointment?.status === "COMPLETED" ||
    appointment?.status === "NO_SHOW";
  const isActionable = Boolean(isTele && !isTerminal);

  const {
    data: teleAccess,
    isLoading: loadingTele,
    refetch: refetchTele,
  } = useQuery({
    queryKey: ["patient-teleconsult-access", idNum],
    queryFn: () => patientPortalApi.getTeleconsultAccess(idNum),
    enabled: Boolean(isActionable && idNum && isAuthenticated()),
    refetchInterval: isActionable ? 15000 : false, // Poll every 15s around meeting time
  });

  const isUnlinked = isUnlinkedProfileError(errorApt);

  if (isUnlinked) {
    return (
      <PatientAppShell>
        <UnlinkedAccountCard
          message={getPortalErrorMessage(errorApt, "No patient clinical profile linked.")}
        />
      </PatientAppShell>
    );
  }

  const handleJoinMeeting = (url: string) => {
    window.open(url, "_blank", "noopener,noreferrer");
  };

  return (
    <PatientAppShell>
      <div className="space-y-6 max-w-3xl mx-auto">
        {/* Top Back link */}
        <Link
          to="/patient/appointments"
          className="inline-flex items-center gap-1.5 text-sm font-bold text-slate-600 hover:text-slate-900 transition-colors"
        >
          <ArrowLeft className="size-4" /> Back to My Appointments
        </Link>

        {/* Loading */}
        {loadingApt && (
          <div className="p-12 text-center bg-white rounded-3xl border border-slate-200">
            <Loader2 className="size-8 animate-spin text-emerald-600 mx-auto mb-3" />
            <p className="text-slate-600 font-semibold">Loading appointment details...</p>
          </div>
        )}

        {/* Error */}
        {errorApt && (
          <div className="p-6 bg-rose-50 border border-rose-200 rounded-3xl text-rose-800 flex items-start gap-3">
            <AlertCircle className="size-6 shrink-0 mt-0.5 text-rose-600" />
            <div className="space-y-1">
              <p className="font-bold">Appointment Not Found</p>
              <p className="text-sm text-rose-700">
                {getPortalErrorMessage(
                  errorApt,
                  "This appointment record could not be found or does not belong to your account.",
                )}
              </p>
              <Button
                variant="outline"
                size="sm"
                onClick={() => refetchApt()}
                className="mt-2 text-rose-900 border-rose-300 hover:bg-rose-100 rounded-xl"
              >
                Retry
              </Button>
            </div>
          </div>
        )}

        {appointment && (
          <div className="space-y-6">
            {/* Header Card */}
            <div className="bg-white p-6 sm:p-8 rounded-3xl border border-slate-200 shadow-xs space-y-4">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <span className="text-sm font-bold text-slate-700 bg-slate-100 px-3 py-1 rounded-xl">
                  Appointment #{appointment.appointmentNumber}
                </span>

                <div className="flex items-center gap-2">
                  {appointment.status === "CONFIRMED" && (
                    <span className="text-xs font-bold text-emerald-800 bg-emerald-100 px-3 py-1 rounded-xl flex items-center gap-1">
                      <CheckCircle2 className="size-4 text-emerald-700" /> Confirmed
                    </span>
                  )}
                  {appointment.status === "COMPLETED" && (
                    <span className="text-xs font-bold text-slate-700 bg-slate-200 px-3 py-1 rounded-xl flex items-center gap-1">
                      <CheckCircle2 className="size-4 text-slate-600" /> Completed
                    </span>
                  )}
                  {appointment.status === "CANCELLED" && (
                    <span className="text-xs font-bold text-rose-800 bg-rose-100 px-3 py-1 rounded-xl flex items-center gap-1">
                      <XCircle className="size-4 text-rose-600" /> Cancelled
                    </span>
                  )}
                  {appointment.status === "IN_PROGRESS" && (
                    <span className="text-xs font-bold text-blue-800 bg-blue-100 px-3 py-1 rounded-xl flex items-center gap-1">
                      <Clock className="size-4 text-blue-600" /> In Progress
                    </span>
                  )}
                </div>
              </div>

              <div>
                <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900">
                  {appointment.doctorName}
                </h1>
                <p className="text-base font-semibold text-emerald-700">
                  {appointment.specialization || "General Medicine"}
                </p>
                <p className="text-sm text-slate-500 flex items-center gap-1.5 mt-1">
                  <Building2 className="size-4" /> {appointment.hospitalName}
                </p>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-4 border-t border-slate-100">
                <div className="bg-slate-50 p-4 rounded-2xl space-y-1">
                  <span className="text-xs font-bold text-slate-500 uppercase tracking-wider block">
                    Scheduled Date
                  </span>
                  <p className="text-base font-bold text-slate-900 flex items-center gap-2">
                    <CalendarDays className="size-4 text-emerald-600" />{" "}
                    {appointment.appointmentDate}
                  </p>
                </div>

                <div className="bg-slate-50 p-4 rounded-2xl space-y-1">
                  <span className="text-xs font-bold text-slate-500 uppercase tracking-wider block">
                    Time Slot
                  </span>
                  <p className="text-base font-bold text-slate-900 flex items-center gap-2">
                    <Clock className="size-4 text-emerald-600" /> {appointment.startTime} -{" "}
                    {appointment.endTime}
                  </p>
                </div>
              </div>

              {appointment.reason && (
                <div className="pt-2 text-sm text-slate-700">
                  <span className="font-bold text-slate-900">Reason for Consultation: </span>
                  {appointment.reason}
                </div>
              )}

              {appointment.cancellationReason && (
                <div className="p-4 bg-rose-50 border border-rose-200 rounded-2xl text-rose-800 text-sm">
                  <span className="font-bold">Cancellation Reason: </span>
                  {appointment.cancellationReason}
                </div>
              )}
            </div>

            {/* Teleconsultation Video Meeting Card (If Online Consultation) */}
            {isTele && (
              <div className="bg-white p-6 sm:p-8 rounded-3xl border border-indigo-200 shadow-sm space-y-4">
                <div className="flex items-center gap-3">
                  <div className="size-12 rounded-2xl bg-indigo-100 text-indigo-700 flex items-center justify-center shrink-0">
                    <Video className="size-6" />
                  </div>
                  <div>
                    <h2 className="text-xl font-bold text-slate-900">Online Video Consultation</h2>
                    <p className="text-xs sm:text-sm text-slate-600">
                      Join from your phone or computer camera and microphone.
                    </p>
                  </div>
                </div>

                {loadingTele ? (
                  <div className="p-4 bg-indigo-50/60 rounded-2xl flex items-center gap-3 text-indigo-900 text-sm">
                    <Loader2 className="size-4 animate-spin" />
                    Checking room access eligibility...
                  </div>
                ) : teleAccess?.canJoin && teleAccess.joinUrl ? (
                  <div className="p-5 bg-emerald-50 border border-emerald-200 rounded-2xl space-y-3">
                    <div className="flex items-center gap-2 text-emerald-900 font-bold text-base">
                      <CheckCircle2 className="size-5 text-emerald-600" /> Video Room is Ready to
                      Join
                    </div>
                    <p className="text-sm text-emerald-800">
                      Your consultation session is now active. Click below to enter the secure video
                      room with your doctor.
                    </p>
                    <Button
                      onClick={() => handleJoinMeeting(teleAccess.joinUrl!)}
                      className="w-full sm:w-auto min-h-[48px] px-6 rounded-xl font-bold text-base bg-emerald-600 hover:bg-emerald-700 text-white shadow-md shadow-emerald-600/20"
                    >
                      <Video className="size-5 mr-2" />
                      Join Video Consultation Now
                      <ExternalLink className="size-4 ml-2" />
                    </Button>
                  </div>
                ) : (
                  <div className="p-5 bg-amber-50/80 border border-amber-200 rounded-2xl space-y-2">
                    <div className="flex items-center gap-2 text-amber-950 font-bold text-sm sm:text-base">
                      <Clock className="size-5 text-amber-600 shrink-0" />
                      Room Not Yet Available
                    </div>
                    <p className="text-sm text-amber-900 leading-relaxed">
                      {teleAccess?.reason ||
                        "The video room opens 15 minutes before the appointment start time and remains accessible up to 60 minutes after."}
                    </p>
                    <p className="text-xs text-amber-800 font-semibold pt-1">
                      This page will automatically refresh as the appointment time approaches.
                    </p>
                  </div>
                )}
              </div>
            )}
          </div>
        )}
      </div>
    </PatientAppShell>
  );
}
