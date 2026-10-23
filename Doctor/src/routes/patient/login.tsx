import { createFileRoute, useNavigate, Link } from "@tanstack/react-router";
import { useState, useEffect } from "react";
import { HeartPulse, Lock, Phone, Mail, Loader2, ArrowRight, ShieldCheck } from "lucide-react";
import { toast } from "sonner";

import { patientPortalApi, getPortalErrorMessage } from "@/services/patient-portal-api";
import { setAccessToken, setAuthUser, isAuthenticated, isPatient, isStaff } from "@/lib/auth";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

export const Route = createFileRoute("/patient/login")({
  head: () => ({
    meta: [
      { title: "Patient Sign In — Meridian Health Portal" },
      {
        name: "description",
        content: "Sign in to view your appointments, prescriptions, and health records.",
      },
    ],
  }),
  component: PatientLoginPage,
});

function PatientLoginPage() {
  const navigate = useNavigate();
  const [emailOrMobile, setEmailOrMobile] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (isAuthenticated() && isPatient()) {
      navigate({ to: "/patient/dashboard" });
    }
  }, [navigate]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!emailOrMobile.trim() || !password) {
      toast.error("Please enter your registered mobile number (or email) and password.");
      return;
    }

    setLoading(true);
    try {
      const res = await patientPortalApi.login(emailOrMobile.trim(), password);

      const hasPatientRole = (res.roles || []).some(
        (r) => r.toUpperCase() === "PATIENT" || r.toUpperCase() === "ROLE_PATIENT",
      );

      if (!hasPatientRole) {
        toast.warning("Staff Account Detected: Please use the Hospital Staff login page.");
        navigate({ to: "/login" });
        return;
      }

      setAccessToken(res.accessToken);
      setAuthUser({
        userId: res.userId,
        email: res.email || emailOrMobile,
        firstName: res.firstName || "Patient",
        lastName: res.lastName || "",
        hospitalId: res.hospitalId,
        roles: res.roles,
      });

      toast.success("Welcome back! Signed in successfully.");
      navigate({ to: "/patient/dashboard" });
    } catch (err: unknown) {
      toast.error(
        getPortalErrorMessage(err, "Invalid mobile number/email or password. Please try again."),
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col justify-center px-4 py-8 sm:py-12">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        {/* Brand Header */}
        <div className="text-center">
          <div className="inline-flex size-16 items-center justify-center rounded-3xl bg-gradient-to-tr from-emerald-600 to-teal-500 text-white shadow-lg shadow-emerald-500/20 mb-4">
            <HeartPulse className="size-9 stroke-[2.25]" />
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-slate-900">
            Patient Portal
          </h1>
          <p className="mt-2 text-sm text-slate-600">
            Sign in to access your appointments, prescriptions, and medical records.
          </p>
        </div>

        {/* Card */}
        <div className="mt-8 bg-white p-6 sm:p-8 rounded-3xl border border-slate-200 shadow-sm">
          <form onSubmit={handleSubmit} className="space-y-5">
            <div className="space-y-2">
              <Label htmlFor="emailOrMobile" className="text-sm font-bold text-slate-800">
                Mobile Number or Email
              </Label>
              <div className="relative">
                <Phone className="absolute left-3.5 top-3.5 size-4 text-slate-400" />
                <Input
                  id="emailOrMobile"
                  type="text"
                  placeholder="e.g. 9876543210 or your@email.com"
                  className="pl-10 h-12 rounded-xl text-base border-slate-300 focus-visible:ring-emerald-500"
                  value={emailOrMobile}
                  onChange={(e) => setEmailOrMobile(e.target.value)}
                  autoComplete="username"
                  required
                />
              </div>
              <p className="text-xs text-slate-500">
                Use the phone number you registered with the hospital.
              </p>
            </div>

            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <Label htmlFor="password" className="text-sm font-bold text-slate-800">
                  Password
                </Label>
              </div>
              <div className="relative">
                <Lock className="absolute left-3.5 top-3.5 size-4 text-slate-400" />
                <Input
                  id="password"
                  type="password"
                  placeholder="Enter your password"
                  className="pl-10 h-12 rounded-xl text-base border-slate-300 focus-visible:ring-emerald-500"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  autoComplete="current-password"
                  required
                />
              </div>
            </div>

            <Button
              type="submit"
              disabled={loading}
              className="w-full h-12 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-base shadow-sm transition-all"
            >
              {loading ? (
                <>
                  <Loader2 className="mr-2 size-5 animate-spin" />
                  Signing In...
                </>
              ) : (
                <>
                  Sign In <ArrowRight className="ml-2 size-5" />
                </>
              )}
            </Button>

            <div className="pt-3 text-center border-t border-slate-100">
              <p className="text-sm text-slate-600">
                New patient?{" "}
                <Link
                  to="/patient/register"
                  className="font-bold text-emerald-700 hover:text-emerald-800 hover:underline"
                >
                  Create an account
                </Link>
              </p>
              <p className="text-xs text-slate-500 pt-1">
                Have an activation code from hospital reception?{" "}
                <Link
                  to="/patient/activate"
                  className="font-bold text-indigo-700 hover:text-indigo-800 hover:underline"
                >
                  Activate Your Account
                </Link>
              </p>
            </div>
          </form>
        </div>

        {/* Security & Staff Link */}
        <div className="mt-6 text-center space-y-2">
          <p className="text-xs text-slate-500 flex items-center justify-center gap-1.5">
            <ShieldCheck className="size-4 text-emerald-600" /> Secure HIPAA-Compliant Healthcare
            Portal
          </p>
          <div>
            <Link
              to="/login"
              className="text-xs font-semibold text-slate-500 hover:text-slate-800 transition-colors"
            >
              Hospital Staff or Doctor? Go to Staff Login &rarr;
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
