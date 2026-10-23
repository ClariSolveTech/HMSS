import { ShieldAlert, PhoneCall, Building2, UserCheck } from "lucide-react";
import { Button } from "@/components/ui/button";
import { logoutPatient } from "@/lib/auth";

interface UnlinkedAccountCardProps {
  message?: string;
}

export function UnlinkedAccountCard({ message }: UnlinkedAccountCardProps) {
  return (
    <div className="max-w-xl mx-auto my-8 p-6 sm:p-8 bg-amber-50/90 border-2 border-amber-300 rounded-3xl shadow-sm text-slate-800">
      <div className="flex items-start gap-4">
        <div className="p-3 bg-amber-200/80 text-amber-900 rounded-2xl shrink-0">
          <ShieldAlert className="size-8" />
        </div>
        <div className="space-y-3">
          <h2 className="text-xl font-bold text-amber-950">
            Clinical Record Verification Required
          </h2>
          <p className="text-sm sm:text-base leading-relaxed text-amber-900 font-medium">
            {message ||
              "Your portal account has been created, but it is not yet connected to an active hospital medical record."}
          </p>

          <div className="bg-white/80 p-4 rounded-2xl border border-amber-200/70 space-y-2.5 text-sm text-slate-700">
            <h3 className="font-bold text-slate-900 flex items-center gap-2">
              <UserCheck className="size-4 text-emerald-600" /> What to do next:
            </h3>
            <ul className="space-y-1.5 list-disc list-inside text-slate-700 text-sm">
              <li>Visit the reception desk at your registered hospital.</li>
              <li>Present your government ID or registered mobile number.</li>
              <li>The receptionist will link your portal login with your medical history.</li>
            </ul>
          </div>

          <div className="pt-2 flex flex-wrap gap-3">
            <Button
              variant="outline"
              onClick={() => logoutPatient()}
              className="min-h-[44px] rounded-xl font-bold border-amber-300 text-amber-950 hover:bg-amber-100"
            >
              Sign Out
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
