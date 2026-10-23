import { createFileRoute, useNavigate, Link } from "@tanstack/react-router";
import { useState, useEffect } from "react";
import {
  HeartPulse,
  Lock,
  Phone,
  Mail,
  User,
  Calendar,
  Building2,
  Loader2,
  ArrowRight,
  CheckCircle2,
  ShieldCheck,
  AlertCircle,
} from "lucide-react";
import { toast } from "sonner";

import {
  patientPortalApi,
  getPortalErrorMessage,
  type HospitalOption,
} from "@/services/patient-portal-api";
import { setAccessToken, setAuthUser } from "@/lib/auth";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

export const Route = createFileRoute("/patient/register")({
  head: () => ({
    meta: [
      { title: "Register Patient Account — Meridian Health Portal" },
      {
        name: "description",
        content: "Create your patient account to book appointments and view records online.",
      },
    ],
  }),
  component: PatientRegisterPage,
});

function PatientRegisterPage() {
  const navigate = useNavigate();

  // Form State
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [dateOfBirth, setDateOfBirth] = useState("");
  const [gender, setGender] = useState<"MALE" | "FEMALE" | "OTHER">("MALE");
  const [bloodGroup, setBloodGroup] = useState<string>("UNKNOWN");
  const [mobileNumber, setMobileNumber] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [hospitalId, setHospitalId] = useState<number | "">("");
  const [city, setCity] = useState("");
  const [address, setAddress] = useState("");

  const [hospitals, setHospitals] = useState<HospitalOption[]>([]);
  const [loadingHospitals, setLoadingHospitals] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    let isMounted = true;
    async function loadHospitals() {
      try {
        const list = await patientPortalApi.getAvailableHospitals();
        if (isMounted) {
          setHospitals(list || []);
          if (list && list.length > 0 && list[0]?.id) {
            setHospitalId(list[0].id);
          }
        }
      } catch (e) {
        // Fallback default
        if (isMounted) {
          setHospitalId(1);
        }
      } finally {
        if (isMounted) setLoadingHospitals(false);
      }
    }
    loadHospitals();
    return () => {
      isMounted = false;
    };
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!firstName.trim() || !lastName.trim()) {
      toast.error("Please enter both your first and last name.");
      return;
    }
    if (!dateOfBirth) {
      toast.error("Please provide your date of birth.");
      return;
    }
    if (!mobileNumber.trim() || mobileNumber.trim().length < 10) {
      toast.error("Please enter a valid 10-digit mobile number.");
      return;
    }
    if (!password || password.length < 6) {
      toast.error("Password must be at least 6 characters long.");
      return;
    }
    if (password !== confirmPassword) {
      toast.error("Passwords do not match. Please re-enter.");
      return;
    }
    if (!hospitalId) {
      toast.error("Please select your primary hospital.");
      return;
    }

    setSubmitting(true);
    try {
      const res = await patientPortalApi.register({
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        dateOfBirth,
        gender,
        bloodGroup: bloodGroup !== "UNKNOWN" ? bloodGroup : undefined,
        mobileNumber: mobileNumber.trim(),
        email: email.trim() ? email.trim() : undefined,
        password,
        hospitalId: Number(hospitalId),
        city: city.trim() ? city.trim() : undefined,
        address: address.trim() ? address.trim() : undefined,
      });

      setAccessToken(res.accessToken);
      setAuthUser({
        userId: res.userId,
        email: res.email || email.trim() || mobileNumber.trim(),
        firstName: res.firstName || firstName.trim(),
        lastName: res.lastName || lastName.trim(),
        hospitalId: res.hospitalId,
        roles: res.roles,
      });

      toast.success("Account created successfully! Welcome to your Patient Portal.");
      navigate({ to: "/patient/dashboard" });
    } catch (err: unknown) {
      toast.error(
        getPortalErrorMessage(err, "Failed to create account. Please check your information."),
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col justify-center px-4 py-8 sm:py-12">
      <div className="sm:mx-auto sm:w-full sm:max-w-2xl">
        {/* Brand Header */}
        <div className="text-center">
          <div className="inline-flex size-16 items-center justify-center rounded-3xl bg-gradient-to-tr from-emerald-600 to-teal-500 text-white shadow-lg shadow-emerald-500/20 mb-4">
            <HeartPulse className="size-9 stroke-[2.25]" />
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-slate-900">
            Create Your Patient Account
          </h1>
          <p className="mt-2 text-sm text-slate-600">
            Register to view your doctor consultations, lab results, and prescriptions online.
          </p>
        </div>

        {/* Card */}
        <div className="mt-8 bg-white p-6 sm:p-10 rounded-3xl border border-slate-200 shadow-sm">
          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Step 1: Personal Details */}
            <div>
              <h2 className="text-base font-bold text-slate-900 mb-4 flex items-center gap-2">
                <User className="size-5 text-emerald-600" /> Personal Information
              </h2>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="firstName" className="text-sm font-semibold text-slate-700">
                    First Name <span className="text-rose-500">*</span>
                  </Label>
                  <Input
                    id="firstName"
                    type="text"
                    placeholder="e.g. Rahul"
                    className="h-12 rounded-xl text-base"
                    value={firstName}
                    onChange={(e) => setFirstName(e.target.value)}
                    required
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="lastName" className="text-sm font-semibold text-slate-700">
                    Last Name <span className="text-rose-500">*</span>
                  </Label>
                  <Input
                    id="lastName"
                    type="text"
                    placeholder="e.g. Sharma"
                    className="h-12 rounded-xl text-base"
                    value={lastName}
                    onChange={(e) => setLastName(e.target.value)}
                    required
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mt-4">
                <div className="space-y-2">
                  <Label htmlFor="dateOfBirth" className="text-sm font-semibold text-slate-700">
                    Date of Birth <span className="text-rose-500">*</span>
                  </Label>
                  <Input
                    id="dateOfBirth"
                    type="date"
                    className="h-12 rounded-xl text-base"
                    value={dateOfBirth}
                    onChange={(e) => setDateOfBirth(e.target.value)}
                    max={new Date().toISOString().split("T")[0]}
                    required
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="gender" className="text-sm font-semibold text-slate-700">
                    Gender <span className="text-rose-500">*</span>
                  </Label>
                  <select
                    id="gender"
                    value={gender}
                    onChange={(e) => setGender(e.target.value as "MALE" | "FEMALE" | "OTHER")}
                    className="w-full h-12 rounded-xl border border-input bg-background px-3 py-2 text-base ring-offset-background focus:outline-hidden focus:ring-2 focus:ring-emerald-500"
                    required
                  >
                    <option value="MALE">Male</option>
                    <option value="FEMALE">Female</option>
                    <option value="OTHER">Other</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mt-4">
                <div className="space-y-2">
                  <Label htmlFor="bloodGroup" className="text-sm font-semibold text-slate-700">
                    Blood Group (Optional)
                  </Label>
                  <select
                    id="bloodGroup"
                    value={bloodGroup}
                    onChange={(e) => setBloodGroup(e.target.value)}
                    className="w-full h-12 rounded-xl border border-input bg-background px-3 py-2 text-base ring-offset-background focus:outline-hidden focus:ring-2 focus:ring-emerald-500"
                  >
                    <option value="UNKNOWN">Don't Know / Prefer not to say</option>
                    <option value="A_POSITIVE">A Positive (A+)</option>
                    <option value="A_NEGATIVE">A Negative (A-)</option>
                    <option value="B_POSITIVE">B Positive (B+)</option>
                    <option value="B_NEGATIVE">B Negative (B-)</option>
                    <option value="O_POSITIVE">O Positive (O+)</option>
                    <option value="O_NEGATIVE">O Negative (O-)</option>
                    <option value="AB_POSITIVE">AB Positive (AB+)</option>
                    <option value="AB_NEGATIVE">AB Negative (AB-)</option>
                  </select>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="city" className="text-sm font-semibold text-slate-700">
                    City / Town
                  </Label>
                  <Input
                    id="city"
                    type="text"
                    placeholder="e.g. Bengaluru"
                    className="h-12 rounded-xl text-base"
                    value={city}
                    onChange={(e) => setCity(e.target.value)}
                  />
                </div>
              </div>
            </div>

            {/* Step 2: Contact & Hospital */}
            <div className="pt-4 border-t border-slate-100">
              <h2 className="text-base font-bold text-slate-900 mb-4 flex items-center gap-2">
                <Building2 className="size-5 text-emerald-600" /> Hospital & Contact Info
              </h2>

              <div className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="hospitalId" className="text-sm font-semibold text-slate-700">
                    Select Your Hospital / Clinic <span className="text-rose-500">*</span>
                  </Label>
                  <select
                    id="hospitalId"
                    value={hospitalId}
                    onChange={(e) => setHospitalId(Number(e.target.value))}
                    className="w-full h-12 rounded-xl border border-input bg-background px-3 py-2 text-base ring-offset-background focus:outline-hidden focus:ring-2 focus:ring-emerald-500"
                    required
                  >
                    {loadingHospitals ? (
                      <option value="">Loading hospitals...</option>
                    ) : hospitals.length > 0 ? (
                      hospitals.map((h) => (
                        <option key={h.id} value={h.id}>
                          {h.name} ({h.code}) {h.city ? `— ${h.city}` : ""}
                        </option>
                      ))
                    ) : (
                      <option value="1">City Central Hospital (Main Branch)</option>
                    )}
                  </select>
                  <p className="text-xs text-slate-500 flex items-center gap-1">
                    <AlertCircle className="size-3.5 text-amber-600" />
                    Note: Your medical records will be connected to this hospital facility.
                  </p>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="mobileNumber" className="text-sm font-semibold text-slate-700">
                      Mobile Number <span className="text-rose-500">*</span>
                    </Label>
                    <div className="relative">
                      <Phone className="absolute left-3.5 top-3.5 size-4 text-slate-400" />
                      <Input
                        id="mobileNumber"
                        type="tel"
                        placeholder="10-digit number"
                        className="pl-10 h-12 rounded-xl text-base"
                        value={mobileNumber}
                        onChange={(e) => setMobileNumber(e.target.value)}
                        required
                      />
                    </div>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="email" className="text-sm font-semibold text-slate-700">
                      Email Address (Optional)
                    </Label>
                    <div className="relative">
                      <Mail className="absolute left-3.5 top-3.5 size-4 text-slate-400" />
                      <Input
                        id="email"
                        type="email"
                        placeholder="your@email.com"
                        className="pl-10 h-12 rounded-xl text-base"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                      />
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {/* Step 3: Password */}
            <div className="pt-4 border-t border-slate-100">
              <h2 className="text-base font-bold text-slate-900 mb-4 flex items-center gap-2">
                <Lock className="size-5 text-emerald-600" /> Account Security
              </h2>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="password" className="text-sm font-semibold text-slate-700">
                    Password (min. 6 characters) <span className="text-rose-500">*</span>
                  </Label>
                  <Input
                    id="password"
                    type="password"
                    placeholder="Create a password"
                    className="h-12 rounded-xl text-base"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="confirmPassword" className="text-sm font-semibold text-slate-700">
                    Confirm Password <span className="text-rose-500">*</span>
                  </Label>
                  <Input
                    id="confirmPassword"
                    type="password"
                    placeholder="Repeat password"
                    className="h-12 rounded-xl text-base"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    required
                  />
                </div>
              </div>
            </div>

            <Button
              type="submit"
              disabled={submitting}
              className="w-full h-13 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-base shadow-sm mt-6"
            >
              {submitting ? (
                <>
                  <Loader2 className="mr-2 size-5 animate-spin" />
                  Creating Account...
                </>
              ) : (
                <>
                  Complete Registration <ArrowRight className="ml-2 size-5" />
                </>
              )}
            </Button>

            <div className="pt-3 text-center border-t border-slate-100">
              <p className="text-sm text-slate-600">
                Already have a patient account?{" "}
                <Link
                  to="/patient/login"
                  className="font-bold text-emerald-700 hover:text-emerald-800 hover:underline"
                >
                  Sign in here
                </Link>
              </p>
            </div>
          </form>
        </div>

        <p className="mt-6 text-center text-xs text-slate-500 flex items-center justify-center gap-1.5">
          <ShieldCheck className="size-4 text-emerald-600" /> Your health data is securely
          encrypted.
        </p>
      </div>
    </div>
  );
}
