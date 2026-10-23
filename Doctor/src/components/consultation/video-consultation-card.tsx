import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  Video,
  ExternalLink,
  CheckCircle2,
  AlertCircle,
  Clock,
  Copy,
  Check,
  Loader2,
  CalendarDays,
  ShieldCheck,
  RefreshCw,
  XCircle,
} from "lucide-react";
import { toast } from "sonner";

import { appointmentApi, type AppointmentResponse } from "@/services/appointment-api";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";

interface VideoConsultationCardProps {
  appointment: AppointmentResponse;
  onAppointmentUpdated?: () => void;
}

export function VideoConsultationCard({
  appointment,
  onAppointmentUpdated,
}: VideoConsultationCardProps) {
  const queryClient = useQueryClient();
  const [copied, setCopied] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const startMutation = useMutation({
    mutationFn: (id: number) => appointmentApi.startOrProvisionTeleconsultation(id),
    onSuccess: (updated) => {
      setErrorMessage(null);
      toast.success("Video room ready! Opening meeting in new tab...");
      queryClient.invalidateQueries({ queryKey: ["consultation-appointment"] });
      queryClient.invalidateQueries({ queryKey: ["today-appointments"] });
      queryClient.invalidateQueries({ queryKey: ["doctor-queue"] });
      if (onAppointmentUpdated) {
        onAppointmentUpdated();
      }
      if (updated.meetUrl) {
        window.open(updated.meetUrl, "_blank", "noopener,noreferrer");
      }
    },
    onError: (err: unknown) => {
      const msg =
        err instanceof Error
          ? err.message
          : "Failed to provision video consultation room. Please try again.";
      setErrorMessage(msg);
      toast.error(msg);
    },
  });

  // Only render for teleconsultations
  if (appointment.appointmentType !== "TELECONSULTATION") {
    return null;
  }

  const hasMeetUrl = Boolean(appointment.meetUrl && appointment.meetUrl.trim() !== "");
  const isTerminal =
    appointment.status === "COMPLETED" ||
    appointment.status === "CANCELLED" ||
    appointment.status === "NO_SHOW";

  const handleCopyLink = () => {
    if (!appointment.meetUrl) return;
    navigator.clipboard.writeText(appointment.meetUrl);
    setCopied(true);
    toast.success("Meeting link copied to clipboard");
    setTimeout(() => setCopied(false), 2000);
  };

  const handleJoinMeeting = () => {
    if (appointment.meetUrl) {
      window.open(appointment.meetUrl, "_blank", "noopener,noreferrer");
    } else {
      startMutation.mutate(appointment.id);
    }
  };

  return (
    <section className="relative overflow-hidden rounded-2xl border-2 border-indigo-200 bg-gradient-to-r from-indigo-50/80 via-white to-blue-50/80 p-5 shadow-xs">
      {/* Decorative accent background pill */}
      <div className="absolute right-0 top-0 -mt-4 -mr-4 size-32 rounded-full bg-indigo-500/5 blur-2xl pointer-events-none" />

      <div className="space-y-4">
        {/* Header Row */}
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-indigo-600 text-white shadow-sm shadow-indigo-600/20">
              <Video className="size-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-base font-extrabold text-slate-900">Video Consultation</h3>
                <Badge
                  variant="outline"
                  className="rounded-full border-indigo-300 bg-indigo-100/70 text-[11px] font-bold text-indigo-900"
                >
                  Teleconsultation
                </Badge>
              </div>
              <p className="text-xs text-slate-600">
                Appointment #{appointment.appointmentNumber} · {appointment.patientName}
              </p>
            </div>
          </div>

          {/* Status Badge */}
          <div className="flex items-center gap-2">
            {appointment.status === "COMPLETED" && (
              <Badge className="bg-slate-200 text-slate-700 border-0 rounded-full text-xs font-semibold">
                <CheckCircle2 className="size-3.5 mr-1" /> Completed
              </Badge>
            )}
            {appointment.status === "CANCELLED" && (
              <Badge className="bg-rose-100 text-rose-800 border-0 rounded-full text-xs font-semibold">
                <XCircle className="size-3.5 mr-1" /> Cancelled
              </Badge>
            )}
            {appointment.status === "NO_SHOW" && (
              <Badge className="bg-amber-100 text-amber-800 border-0 rounded-full text-xs font-semibold">
                No Show
              </Badge>
            )}
            {!isTerminal && hasMeetUrl && (
              <Badge className="bg-emerald-100 text-emerald-800 border-emerald-300 rounded-full text-xs font-bold animate-pulse">
                <CheckCircle2 className="size-3.5 mr-1 text-emerald-600" /> Room Ready
              </Badge>
            )}
            {!isTerminal && !hasMeetUrl && (
              <Badge className="bg-blue-100 text-blue-800 border-blue-200 rounded-full text-xs font-semibold">
                <Clock className="size-3.5 mr-1 text-blue-600" /> Room Not Started
              </Badge>
            )}
          </div>
        </div>

        {/* Schedule & Timing Info */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-1">
          <div className="flex items-center gap-2 text-xs font-medium text-slate-700 bg-white/70 p-2.5 rounded-xl border border-indigo-100">
            <CalendarDays className="size-4 text-indigo-600 shrink-0" />
            <span>
              Scheduled Date: <strong>{appointment.appointmentDate}</strong>
            </span>
          </div>
          <div className="flex items-center gap-2 text-xs font-medium text-slate-700 bg-white/70 p-2.5 rounded-xl border border-indigo-100">
            <Clock className="size-4 text-indigo-600 shrink-0" />
            <span>
              Slot:{" "}
              <strong>
                {appointment.startTime?.substring(0, 5)} - {appointment.endTime?.substring(0, 5)}
              </strong>
            </span>
          </div>
        </div>

        {/* Error State */}
        {errorMessage && (
          <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-rose-800 text-xs flex items-start gap-2">
            <AlertCircle className="size-4 shrink-0 text-rose-600 mt-0.5" />
            <div className="flex-1 space-y-1">
              <p className="font-bold">Teleconsultation Provisioning Error</p>
              <p className="text-rose-700 leading-relaxed">{errorMessage}</p>
            </div>
            <Button
              variant="outline"
              size="sm"
              onClick={() => startMutation.mutate(appointment.id)}
              className="h-7 text-xs border-rose-300 text-rose-900 hover:bg-rose-100 rounded-lg shrink-0"
            >
              <RefreshCw className="size-3 mr-1" /> Retry
            </Button>
          </div>
        )}

        {/* Action Controls */}
        <div className="pt-2">
          {appointment.status === "COMPLETED" ? (
            <div className="rounded-xl bg-slate-100/90 p-3 text-xs text-slate-700 flex items-center gap-2">
              <CheckCircle2 className="size-4 text-slate-600 shrink-0" />
              <span>
                This online consultation has already been completed. Clinical notes, prescription,
                and billing are archived below.
              </span>
            </div>
          ) : appointment.status === "CANCELLED" ? (
            <div className="rounded-xl bg-rose-50 p-3 text-xs text-rose-800 flex items-center gap-2">
              <XCircle className="size-4 text-rose-600 shrink-0" />
              <span>This appointment was cancelled. Live video consultation is disabled.</span>
            </div>
          ) : appointment.status === "NO_SHOW" ? (
            <div className="rounded-xl bg-amber-50 p-3 text-xs text-amber-900 flex items-center gap-2">
              <AlertCircle className="size-4 text-amber-600 shrink-0" />
              <span>Patient was marked as no-show. Video consultation is unavailable.</span>
            </div>
          ) : hasMeetUrl ? (
            /* Meeting is ready - Join Action */
            <div className="space-y-3">
              <div className="flex flex-wrap items-center gap-2.5">
                <Button
                  onClick={handleJoinMeeting}
                  className="min-h-[44px] px-5 rounded-xl font-bold bg-emerald-600 hover:bg-emerald-700 text-white shadow-sm shadow-emerald-600/20"
                >
                  <Video className="size-4 mr-2" />
                  Join Video Consultation
                  <ExternalLink className="size-3.5 ml-2" />
                </Button>

                <Button
                  type="button"
                  variant="outline"
                  onClick={handleCopyLink}
                  className="min-h-[44px] px-4 rounded-xl font-semibold border-slate-300 hover:bg-slate-100 text-slate-800"
                >
                  {copied ? (
                    <>
                      <Check className="size-4 mr-1.5 text-emerald-600" /> Copied!
                    </>
                  ) : (
                    <>
                      <Copy className="size-4 mr-1.5 text-slate-600" /> Copy Room Link
                    </>
                  )}
                </Button>
              </div>

              <div className="flex items-center gap-2 text-[11px] text-slate-500">
                <ShieldCheck className="size-3.5 text-emerald-600 shrink-0" />
                <span>
                  Opens Google Meet in a new tab. Keep this workspace open on your screen to
                  document vitals, diagnosis, and prescriptions simultaneously.
                </span>
              </div>
            </div>
          ) : (
            /* Meeting not yet provisioned - Start Action */
            <div className="space-y-3">
              <Button
                onClick={() => startMutation.mutate(appointment.id)}
                disabled={startMutation.isPending}
                className="min-h-[44px] px-5 rounded-xl font-bold bg-indigo-600 hover:bg-indigo-700 text-white shadow-sm shadow-indigo-600/20"
              >
                {startMutation.isPending ? (
                  <>
                    <Loader2 className="size-4 mr-2 animate-spin" />
                    Provisioning Video Room...
                  </>
                ) : (
                  <>
                    <Video className="size-4 mr-2" />
                    Start Video Consultation
                  </>
                )}
              </Button>

              <p className="text-[11px] text-slate-500">
                Clicking &quot;Start Video Consultation&quot; creates the secure room space and
                launches the call. Both you and the patient will join this room.
              </p>
            </div>
          )}
        </div>
      </div>
    </section>
  );
}
