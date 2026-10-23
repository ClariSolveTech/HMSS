import { createFileRoute } from "@tanstack/react-router";
import { useState, useEffect } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  User,
  Building2,
  Calendar,
  Phone,
  Mail,
  MapPin,
  Shield,
  Loader2,
  AlertCircle,
  Save,
  CheckCircle2,
  Lock,
} from "lucide-react";
import { toast } from "sonner";

import { PatientAppShell } from "@/components/patient/patient-app-shell";
import { UnlinkedAccountCard } from "@/components/patient/unlinked-account-card";
import {
  patientPortalApi,
  getPortalErrorMessage,
  isUnlinkedProfileError,
  type UpdatePatientProfileRequest,
} from "@/services/patient-portal-api";
import { isAuthenticated } from "@/lib/auth";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

export const Route = createFileRoute("/patient/profile")({
  head: () => ({
    meta: [
      { title: "My Profile — Patient Portal" },
      {
        name: "description",
        content: "View and update your contact details and emergency contacts.",
      },
    ],
  }),
  component: PatientProfilePage,
});

function PatientProfilePage() {
  const queryClient = useQueryClient();

  const {
    data: profile,
    isLoading,
    error,
    refetch,
  } = useQuery({
    queryKey: ["patient-profile"],
    queryFn: () => patientPortalApi.getProfile(),
    enabled: isAuthenticated(),
  });

  // Editable fields state
  const [address, setAddress] = useState("");
  const [city, setCity] = useState("");
  const [state, setState] = useState("");
  const [pincode, setPincode] = useState("");
  const [emergencyContactName, setEmergencyContactName] = useState("");
  const [emergencyContactNumber, setEmergencyContactNumber] = useState("");

  useEffect(() => {
    if (profile) {
      setAddress(profile.address || "");
      setCity(profile.city || "");
      setState(profile.state || "");
      setPincode(profile.pincode || "");
      setEmergencyContactName(profile.emergencyContactName || "");
      setEmergencyContactNumber(profile.emergencyContactNumber || "");
    }
  }, [profile]);

  const updateMutation = useMutation({
    mutationFn: (data: UpdatePatientProfileRequest) => patientPortalApi.updateProfile(data),
    onSuccess: (updated) => {
      queryClient.setQueryData(["patient-profile"], updated);
      toast.success("Contact details updated successfully!");
    },
    onError: (err: unknown) => {
      toast.error(getPortalErrorMessage(err, "Failed to update profile. Please try again."));
    },
  });

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();
    updateMutation.mutate({
      address: address.trim() || undefined,
      city: city.trim() || undefined,
      state: state.trim() || undefined,
      pincode: pincode.trim() || undefined,
      emergencyContactName: emergencyContactName.trim() || undefined,
      emergencyContactNumber: emergencyContactNumber.trim() || undefined,
    });
  };

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
      <div className="space-y-6 max-w-3xl mx-auto">
        {/* Header */}
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
            My Patient Profile
          </h1>
          <p className="mt-1 text-sm text-slate-600">
            View your clinical identity information and update your personal contact details.
          </p>
        </div>

        {/* Loading */}
        {isLoading && (
          <div className="p-12 text-center bg-white rounded-3xl border border-slate-200">
            <Loader2 className="size-8 animate-spin text-emerald-600 mx-auto mb-3" />
            <p className="text-slate-600 font-semibold">Loading profile information...</p>
          </div>
        )}

        {/* Error */}
        {error && (
          <div className="p-6 bg-rose-50 border border-rose-200 rounded-3xl text-rose-800 flex items-start gap-3">
            <AlertCircle className="size-6 shrink-0 mt-0.5 text-rose-600" />
            <div className="space-y-1">
              <p className="font-bold">Error loading profile</p>
              <p className="text-sm text-rose-700">
                {getPortalErrorMessage(error, "Failed to retrieve your patient profile.")}
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

        {profile && (
          <div className="space-y-6">
            {/* Section 1: Read-Only Clinical Identity */}
            <div className="bg-white p-6 sm:p-8 rounded-3xl border border-slate-200 shadow-xs space-y-4">
              <div className="flex items-center justify-between border-b border-slate-100 pb-3">
                <div className="flex items-center gap-2">
                  <User className="size-5 text-emerald-600" />
                  <h2 className="text-lg font-bold text-slate-900">Registered Patient Identity</h2>
                </div>
                <span className="text-xs font-bold text-slate-500 bg-slate-100 px-3 py-1 rounded-xl flex items-center gap-1">
                  <Lock className="size-3" /> Verified Clinical Record
                </span>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
                <div className="bg-slate-50 p-3.5 rounded-2xl space-y-1">
                  <span className="text-xs font-bold text-slate-400 uppercase">
                    Patient Full Name
                  </span>
                  <p className="text-base font-bold text-slate-900">
                    {profile.firstName} {profile.lastName}
                  </p>
                </div>

                <div className="bg-slate-50 p-3.5 rounded-2xl space-y-1">
                  <span className="text-xs font-bold text-slate-400 uppercase">
                    Patient ID / Code
                  </span>
                  <p className="text-base font-bold text-slate-900">{profile.patientCode}</p>
                </div>

                <div className="bg-slate-50 p-3.5 rounded-2xl space-y-1">
                  <span className="text-xs font-bold text-slate-400 uppercase">
                    Hospital Facility
                  </span>
                  <p className="text-base font-bold text-slate-900">{profile.hospitalName}</p>
                </div>

                <div className="bg-slate-50 p-3.5 rounded-2xl space-y-1">
                  <span className="text-xs font-bold text-slate-400 uppercase">
                    Date of Birth & Gender
                  </span>
                  <p className="text-base font-bold text-slate-900">
                    {profile.dateOfBirth} ({profile.gender})
                  </p>
                </div>

                <div className="bg-slate-50 p-3.5 rounded-2xl space-y-1">
                  <span className="text-xs font-bold text-slate-400 uppercase">Blood Group</span>
                  <p className="text-base font-bold text-slate-900">
                    {profile.bloodGroup && profile.bloodGroup !== "UNKNOWN"
                      ? profile.bloodGroup.replace("_", " ")
                      : "Not specified"}
                  </p>
                </div>

                <div className="bg-slate-50 p-3.5 rounded-2xl space-y-1">
                  <span className="text-xs font-bold text-slate-400 uppercase">
                    Registered Mobile
                  </span>
                  <p className="text-base font-bold text-slate-900">{profile.mobileNumber}</p>
                </div>
              </div>

              <p className="text-xs text-slate-500 pt-1">
                To update your legal name, gender, or date of birth, please present government
                identification to the hospital reception desk.
              </p>
            </div>

            {/* Section 2: Editable Contact Information */}
            <div className="bg-white p-6 sm:p-8 rounded-3xl border border-slate-200 shadow-xs">
              <div className="flex items-center justify-between border-b border-slate-100 pb-3 mb-5">
                <div className="flex items-center gap-2">
                  <MapPin className="size-5 text-emerald-600" />
                  <h2 className="text-lg font-bold text-slate-900">
                    Contact & Residential Details
                  </h2>
                </div>
                <span className="text-xs font-semibold text-emerald-700">Editable</span>
              </div>

              <form onSubmit={handleSave} className="space-y-5">
                <div className="space-y-2">
                  <Label htmlFor="address" className="text-sm font-semibold text-slate-700">
                    Street Address
                  </Label>
                  <Input
                    id="address"
                    type="text"
                    placeholder="House/Apartment, Street name"
                    className="h-12 rounded-xl text-base"
                    value={address}
                    onChange={(e) => setAddress(e.target.value)}
                  />
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="city" className="text-sm font-semibold text-slate-700">
                      City / District
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

                  <div className="space-y-2">
                    <Label htmlFor="state" className="text-sm font-semibold text-slate-700">
                      State
                    </Label>
                    <Input
                      id="state"
                      type="text"
                      placeholder="e.g. Karnataka"
                      className="h-12 rounded-xl text-base"
                      value={state}
                      onChange={(e) => setState(e.target.value)}
                    />
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="pincode" className="text-sm font-semibold text-slate-700">
                      Pincode / Postal
                    </Label>
                    <Input
                      id="pincode"
                      type="text"
                      placeholder="e.g. 560001"
                      className="h-12 rounded-xl text-base"
                      value={pincode}
                      onChange={(e) => setPincode(e.target.value)}
                    />
                  </div>
                </div>

                {/* Emergency Contact */}
                <div className="pt-4 border-t border-slate-100">
                  <h3 className="text-base font-bold text-slate-900 mb-3 flex items-center gap-2">
                    <Phone className="size-4 text-emerald-600" /> Emergency Contact
                  </h3>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label
                        htmlFor="emergencyContactName"
                        className="text-sm font-semibold text-slate-700"
                      >
                        Contact Person Name
                      </Label>
                      <Input
                        id="emergencyContactName"
                        type="text"
                        placeholder="e.g. Spouse, Parent, Relative"
                        className="h-12 rounded-xl text-base"
                        value={emergencyContactName}
                        onChange={(e) => setEmergencyContactName(e.target.value)}
                      />
                    </div>

                    <div className="space-y-2">
                      <Label
                        htmlFor="emergencyContactNumber"
                        className="text-sm font-semibold text-slate-700"
                      >
                        Contact Phone Number
                      </Label>
                      <Input
                        id="emergencyContactNumber"
                        type="tel"
                        placeholder="10-digit mobile number"
                        className="h-12 rounded-xl text-base"
                        value={emergencyContactNumber}
                        onChange={(e) => setEmergencyContactNumber(e.target.value)}
                      />
                    </div>
                  </div>
                </div>

                <div className="pt-4 flex justify-end">
                  <Button
                    type="submit"
                    disabled={updateMutation.isPending}
                    className="min-h-[46px] w-full sm:w-auto px-6 rounded-xl font-bold bg-emerald-600 hover:bg-emerald-700 text-white shadow-xs"
                  >
                    {updateMutation.isPending ? (
                      <>
                        <Loader2 className="mr-2 size-4 animate-spin" />
                        Saving Changes...
                      </>
                    ) : (
                      <>
                        <Save className="mr-2 size-4" />
                        Save Contact Details
                      </>
                    )}
                  </Button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
    </PatientAppShell>
  );
}
