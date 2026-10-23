import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import { Stethoscope, Lock, Mail, Loader2, ArrowRight } from "lucide-react";
import { toast } from "sonner";

import { queueApi } from "@/services/queue-api";
import { setAccessToken, setAuthUser } from "@/lib/auth";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

export const Route = createFileRoute("/login")({
  head: () => ({
    meta: [
      { title: "Doctor Login — Meridian Health OS" },
      { name: "description", content: "Sign in to access your doctor dashboard and live queue." },
    ],
  }),
  component: LoginPage,
});

function LoginPage() {
  const navigate = useNavigate();
  const [emailOrMobile, setEmailOrMobile] = useState("dr.aarav@citycentral.com");
  const [password, setPassword] = useState("Doctor@123");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!emailOrMobile || !password) {
      toast.error("Please enter email/mobile and password");
      return;
    }

    setLoading(true);
    try {
      const loginRes = await queueApi.login(emailOrMobile, password);
      setAccessToken(loginRes.accessToken);

      const hasRecepRole = (loginRes.roles || []).some(
        (r) => r.toUpperCase() === "RECEPTIONIST" || r.toUpperCase() === "ROLE_RECEPTIONIST"
      );
      const hasDoctorRole = (loginRes.roles || []).some(
        (r) => r.toUpperCase() === "DOCTOR" || r.toUpperCase() === "ROLE_DOCTOR"
      );

      // Fetch doctor profile if doctor role
      let doctorId: number | undefined;
      if (hasDoctorRole) {
        try {
          const doctorProfile = await queueApi.getDoctorProfileMe();
          doctorId = doctorProfile.id;
        } catch {
          // If not doctor profile found yet, user info is still available
        }
      }

      setAuthUser({
        userId: loginRes.userId,
        email: emailOrMobile,
        firstName: loginRes.firstName || (hasRecepRole ? "Maya" : "Doctor"),
        lastName: loginRes.lastName || (hasRecepRole ? "Sharma" : "Aarav"),
        hospitalId: loginRes.hospitalId,
        roles: loginRes.roles,
        ...(doctorId !== undefined ? { doctorId } : {}),
      });

      const hasPatientRole = (loginRes.roles || []).some(
        (r) => r.toUpperCase() === "PATIENT" || r.toUpperCase() === "ROLE_PATIENT"
      );

      toast.success("Successfully logged in!");
      if (hasPatientRole && !hasDoctorRole && !hasRecepRole) {
        navigate({ to: "/patient/dashboard" });
      } else if (hasRecepRole && !hasDoctorRole) {
        navigate({ to: "/receptionist" });
      } else {
        navigate({ to: "/" });
      }
    } catch (err: any) {
      toast.error(err.message || "Invalid credentials. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  const setRoleDemo = (role: "doctor" | "receptionist") => {
    if (role === "doctor") {
      setEmailOrMobile("dr.aarav@citycentral.com");
      setPassword("Doctor@123");
    } else {
      setEmailOrMobile("receptionist@citycentral.com");
      setPassword("Recep@123");
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-background px-4 py-12">
      <div className="w-full max-w-md space-y-6">
        <div className="text-center">
          <div className="mx-auto grid size-12 place-items-center rounded-2xl brand-gradient text-primary-foreground shadow-float mb-4">
            <Stethoscope className="size-6" />
          </div>
          <h1 className="text-2xl font-extrabold tracking-tight">Meridian Sign In</h1>
          <p className="mt-1.5 text-sm text-muted-foreground">
            Meridian Health OS · City Central Hospital
          </p>
          <div className="mt-3 flex justify-center gap-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="rounded-full text-xs"
              onClick={() => setRoleDemo("doctor")}
            >
              Doctor Demo
            </Button>
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="rounded-full text-xs"
              onClick={() => setRoleDemo("receptionist")}
            >
              Receptionist Demo
            </Button>
          </div>
        </div>

        <div className="surface-card rounded-2xl p-6 sm:p-8 shadow-sm">
          <form onSubmit={handleSubmit} className="space-y-5">
            <div className="space-y-2">
              <Label htmlFor="email">Email or Mobile Number</Label>
              <div className="relative">
                <Mail className="absolute left-3 top-3 size-4 text-muted-foreground" />
                <Input
                  id="email"
                  type="text"
                  placeholder="dr.aarav@citycentral.com"
                  className="pl-9 rounded-xl"
                  value={emailOrMobile}
                  onChange={(e) => setEmailOrMobile(e.target.value)}
                  required
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="password">Password</Label>
              <div className="relative">
                <Lock className="absolute left-3 top-3 size-4 text-muted-foreground" />
                <Input
                  id="password"
                  type="password"
                  placeholder="••••••••"
                  className="pl-9 rounded-xl"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />
              </div>
            </div>

            <Button type="submit" disabled={loading} className="w-full rounded-xl py-5 font-semibold">
              {loading ? (
                <>
                  <Loader2 className="mr-2 size-4 animate-spin" />
                  Signing in...
                </>
              ) : (
                <>
                  Sign In <ArrowRight className="ml-2 size-4" />
                </>
              )}
            </Button>

            <div className="pt-2 text-center text-sm border-t">
              <span className="text-muted-foreground">Are you a patient? </span>
              <button
                type="button"
                onClick={() => navigate({ to: "/patient/login" })}
                className="font-medium text-primary hover:underline"
              >
                Go to Patient Portal &rarr;
              </button>
            </div>
          </form>
        </div>

        <p className="text-center text-xs text-muted-foreground">
          Protected by Meridian Multi-Tenant Hospital Security
        </p>
      </div>
    </div>
  );
}
