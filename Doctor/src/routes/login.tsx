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

      // Fetch doctor profile if doctor role
      let doctorId: number | undefined;
      try {
        const doctorProfile = await queueApi.getDoctorProfileMe();
        doctorId = doctorProfile.id;
      } catch {
        // If not doctor profile found yet, user info is still available
      }

      setAuthUser({
        userId: loginRes.userId,
        email: emailOrMobile,
        firstName: "Doctor",
        lastName: "",
        hospitalId: loginRes.hospitalId,
        roles: loginRes.roles,
        ...(doctorId !== undefined ? { doctorId } : {}),
      });

      toast.success("Successfully logged in!");
      navigate({ to: "/" });
    } catch (err: any) {
      toast.error(err.message || "Invalid credentials. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-background px-4 py-12">
      <div className="w-full max-w-md space-y-6">
        <div className="text-center">
          <div className="mx-auto grid size-12 place-items-center rounded-2xl brand-gradient text-primary-foreground shadow-float mb-4">
            <Stethoscope className="size-6" />
          </div>
          <h1 className="text-2xl font-extrabold tracking-tight">Doctor Sign In</h1>
          <p className="mt-1.5 text-sm text-muted-foreground">
            Meridian Health OS · Live OPD Queue & Consultation
          </p>
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
          </form>
        </div>

        <p className="text-center text-xs text-muted-foreground">
          Protected by Meridian Multi-Tenant Hospital Security
        </p>
      </div>
    </div>
  );
}
