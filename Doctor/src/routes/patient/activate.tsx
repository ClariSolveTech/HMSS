import { createFileRoute, useNavigate, Link } from "@tanstack/react-router";
import { useState, useEffect } from "react";
import {
  HeartPulse,
  KeyRound,
  Lock,
  Mail,
  Loader2,
  ArrowRight,
  ShieldCheck,
  CheckCircle2,
} from "lucide-react";
import { toast } from "sonner";

import { patientPortalApi, getPortalErrorMessage } from "@/services/patient-portal-api";
import { setAccessToken, setAuthUser, isAuthenticated, isPatient } from "@/lib/auth";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

export const Route = createFileRoute("/patient/activate")({
  head: () => ({
    meta: [
      { title: "Activate Patient Account — Meridian Health Portal" },
      {
        name: "description",
        content: "Activate your patient portal account using your hospital-issued activation code.",
      },
    ],
  }),
  component: PatientActivatePage,
});

function PatientActivatePage() {
  const navigate = useNavigate();
  const [activationCode, setActivationCode] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  useEffect(() => {
    if (isAuthenticated() && isPatient()) {
      navigate({ to: "/patient/appointments" });
    }
  }, [navigate]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    const cleanCode = activationCode.trim().toUpperCase();
    if (!cleanCode) {
      const err = "Please enter the activation code provided by hospital reception.";
      setFormError(err);
      toast.error(err);
      return;
    }

    if (password.length < 8) {
      const err = "Password must be at least 8 characters long.";
      setFormError(err);
      toast.error(err);
      return;
    }

    if (password !== confirmPassword) {
      const err = "Passwords do not match. Please re-enter.";
      setFormError(err);
      toast.error(err);
      return;
    }

    setLoading(true);
    try {
      const res = await patientPortalApi.activate({
        activationCode: cleanCode,
        password,
        email: email.trim() || undefined,
      });

      setAccessToken(res.accessToken);
      setAuthUser({
        userId: res.userId,
        email: res.email || email.trim(),
        firstName: res.firstName || "Patient",
        lastName: res.lastName || "",
        hospitalId: res.hospitalId,
        roles: res.roles,
      });

      toast.success("Account activated successfully! Welcome to your Patient Portal.");
      navigate({ to: "/patient/appointments" });
    } catch (err: unknown) {
      const msg = getPortalErrorMessage(
        err,
        "Invalid or expired activation code. Please verify the code or visit reception.",
      );
      setFormError(msg);
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col justify-center px-4 py-8 sm:py-12">
      <div className="max-w-md w-full mx-auto space-y-6">
        {/* Brand header */}
        <div className="text-center space-y-2">
          <div className="inline-flex size-14 items-center justify-center rounded-2xl bg-indigo-600 text-white shadow-lg shadow-indigo-600/30">
            <HeartPulse className="size-7" />
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
            Activate Your Account
          </h1>
          <p className="text-sm text-slate-600">
            Enter the one-time activation code received from hospital reception to create your
            password.
          </p>
        </div>

        {/* Card form */}
        <div className="bg-white p-6 sm:p-8 rounded-3xl border border-slate-200/80 shadow-xl shadow-slate-200/40 space-y-5">
          <form onSubmit={handleSubmit} className="space-y-4">
            {/* Activation Code */}
            <div className="space-y-1.5">
              <Label
                htmlFor="activationCode"
                className="text-xs font-bold text-slate-700 uppercase tracking-wider"
              >
                Hospital Activation Code *
              </Label>
              <div className="relative">
                <KeyRound className="absolute left-3.5 top-3 size-4 text-indigo-500" />
                <Input
                  id="activationCode"
                  type="text"
                  required
                  placeholder="e.g. ACT-K9B2-7X4M"
                  value={activationCode}
                  onChange={(e) => {
                    setActivationCode(e.target.value.toUpperCase());
                    setFormError(null);
                  }}
                  className="pl-10 min-h-[46px] rounded-xl font-mono text-base font-bold tracking-wider uppercase"
                />
              </div>
              <p className="text-[11px] text-slate-500">
                Provided by your hospital receptionist or billing desk.
              </p>
            </div>

            {/* Optional Email */}
            <div className="space-y-1.5">
              <Label
                htmlFor="email"
                className="text-xs font-bold text-slate-700 uppercase tracking-wider"
              >
                Email Address (Optional)
              </Label>
              <div className="relative">
                <Mail className="absolute left-3.5 top-3 size-4 text-slate-400" />
                <Input
                  id="email"
                  type="email"
                  placeholder="you@example.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="pl-10 min-h-[44px] rounded-xl"
                />
              </div>
            </div>

            {/* Password */}
            <div className="space-y-1.5">
              <Label
                htmlFor="password"
                className="text-xs font-bold text-slate-700 uppercase tracking-wider"
              >
                Create Portal Password *
              </Label>
              <div className="relative">
                <Lock className="absolute left-3.5 top-3 size-4 text-slate-400" />
                <Input
                  id="password"
                  type="password"
                  required
                  placeholder="Minimum 8 characters"
                  value={password}
                  onChange={(e) => {
                    setPassword(e.target.value);
                    setFormError(null);
                  }}
                  className="pl-10 min-h-[44px] rounded-xl"
                />
              </div>
            </div>

            {/* Confirm Password */}
            <div className="space-y-1.5">
              <Label
                htmlFor="confirmPassword"
                className="text-xs font-bold text-slate-700 uppercase tracking-wider"
              >
                Confirm Password *
              </Label>
              <div className="relative">
                <Lock className="absolute left-3.5 top-3 size-4 text-slate-400" />
                <Input
                  id="confirmPassword"
                  type="password"
                  required
                  placeholder="Repeat your password"
                  value={confirmPassword}
                  onChange={(e) => {
                    setConfirmPassword(e.target.value);
                    setFormError(null);
                  }}
                  className="pl-10 min-h-[44px] rounded-xl"
                />
              </div>
            </div>

            {/* Inline Error */}
            {formError && (
              <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-xs text-rose-800 font-medium">
                {formError}
              </div>
            )}

            <Button
              type="submit"
              disabled={loading}
              className="w-full min-h-[46px] rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-base shadow-md shadow-indigo-600/20"
            >
              {loading ? (
                <>
                  <Loader2 className="size-4 mr-2 animate-spin" /> Activating Account...
                </>
              ) : (
                <>
                  Activate & Open My Appointments <ArrowRight className="size-4 ml-2" />
                </>
              )}
            </Button>
          </form>

          {/* Guarantee badge */}
          <div className="pt-2 flex items-center justify-center gap-2 text-xs text-slate-500">
            <ShieldCheck className="size-4 text-emerald-600" />
            <span>End-to-end encrypted hospital data isolation</span>
          </div>
        </div>

        {/* Footer links */}
        <div className="text-center space-y-2 text-xs text-slate-600">
          <p>
            Already have an active account?{" "}
            <Link to="/patient/login" className="font-bold text-indigo-700 hover:underline">
              Sign In here
            </Link>
          </p>
          <p>
            Don't have an activation code? Ask hospital reception to issue an activation code during
            your visit.
          </p>
        </div>
      </div>
    </div>
  );
}
