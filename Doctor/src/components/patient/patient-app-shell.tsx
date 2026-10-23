import { Link, useRouterState, useNavigate } from "@tanstack/react-router";
import { useEffect, type ReactNode } from "react";
import {
  LayoutDashboard,
  CalendarDays,
  Pill,
  FileText,
  User,
  LogOut,
  HeartPulse,
  Building2,
} from "lucide-react";
import { toast } from "sonner";

import { cn } from "@/lib/utils";
import { getAuthUser, isAuthenticated, isPatient, isStaff, logoutPatient } from "@/lib/auth";
import { Button } from "@/components/ui/button";

interface PatientAppShellProps {
  children: ReactNode;
}

const patientNavItems = [
  { to: "/patient/dashboard", label: "Home", icon: LayoutDashboard },
  { to: "/patient/appointments", label: "Appointments", icon: CalendarDays },
  { to: "/patient/prescriptions", label: "Prescriptions", icon: Pill },
  { to: "/patient/health-records", label: "Health Records", icon: FileText },
  { to: "/patient/profile", label: "My Profile", icon: User },
] as const;

export function PatientAppShell({ children }: PatientAppShellProps) {
  const navigate = useNavigate();
  const pathname = useRouterState({ select: (s) => s.location.pathname });
  const user = getAuthUser();
  const authenticated = isAuthenticated();

  useEffect(() => {
    if (!authenticated) {
      navigate({ to: "/patient/login" });
      return;
    }

    if (!isPatient() && isStaff()) {
      toast.error("Notice: Staff accounts must use the Staff Dashboard");
      navigate({ to: "/" });
    }
  }, [authenticated, navigate]);

  const handleLogout = () => {
    logoutPatient();
  };

  const patientDisplayName = user
    ? `${user.firstName || ""} ${user.lastName || ""}`.trim() ||
      user.email?.split("@")[0] ||
      "Patient"
    : "Patient";

  if (!authenticated || (!isPatient() && isStaff())) {
    return null;
  }

  return (
    <div className="min-h-screen bg-slate-50/70 text-slate-900 pb-20 md:pb-8 flex flex-col font-sans">
      {/* Top Header */}
      <header className="sticky top-0 z-40 bg-white/95 backdrop-blur-md border-b border-slate-200 shadow-xs">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 h-18 flex items-center justify-between gap-4">
          {/* Brand */}
          <Link
            to="/patient/dashboard"
            className="flex items-center gap-3 focus:outline-hidden focus-visible:ring-2 focus-visible:ring-emerald-500 rounded-xl"
            aria-label="Patient Portal Home"
          >
            <div className="w-11 h-11 rounded-2xl bg-gradient-to-tr from-emerald-600 to-teal-500 flex items-center justify-center text-white shadow-md shadow-emerald-500/20 shrink-0">
              <HeartPulse className="size-6 stroke-[2.25]" />
            </div>
            <div>
              <span className="block text-lg font-bold tracking-tight text-slate-900 leading-tight">
                Patient Portal
              </span>
              <span className="block text-xs font-semibold text-emerald-700 leading-tight">
                Meridian Healthcare
              </span>
            </div>
          </Link>

          {/* Desktop Navigation */}
          <nav className="hidden md:flex items-center gap-1 bg-slate-100/90 p-1.5 rounded-2xl border border-slate-200/80">
            {patientNavItems.map((item) => {
              const isActive =
                item.to === "/patient/dashboard"
                  ? pathname === item.to
                  : pathname.startsWith(item.to);
              const Icon = item.icon;
              return (
                <Link
                  key={item.to}
                  to={item.to}
                  className={cn(
                    "flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-semibold transition-all duration-150 min-h-[44px]",
                    isActive
                      ? "bg-white text-emerald-700 shadow-sm shadow-slate-200/60 font-bold"
                      : "text-slate-600 hover:text-slate-900 hover:bg-slate-200/60",
                  )}
                >
                  <Icon
                    className={cn("size-4", isActive ? "text-emerald-600" : "text-slate-400")}
                  />
                  <span>{item.label}</span>
                </Link>
              );
            })}
          </nav>

          {/* User profile & Logout */}
          <div className="flex items-center gap-2">
            <div className="hidden sm:flex flex-col text-right">
              <span className="text-sm font-bold text-slate-900 truncate max-w-[160px]">
                {patientDisplayName}
              </span>
              <span className="text-xs text-slate-500 flex items-center justify-end gap-1">
                <Building2 className="size-3" /> Patient
              </span>
            </div>

            <Button
              variant="ghost"
              size="sm"
              onClick={handleLogout}
              className="min-h-[44px] min-w-[44px] px-3 rounded-xl text-rose-600 hover:text-rose-700 hover:bg-rose-50 border border-transparent hover:border-rose-200"
              aria-label="Log out of patient account"
            >
              <LogOut className="size-4 mr-1.5" />
              <span className="text-xs font-bold">Log out</span>
            </Button>
          </div>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="max-w-6xl w-full mx-auto px-4 sm:px-6 pt-6 flex-1">{children}</main>

      {/* Mobile Bottom Navigation Bar (Large touch targets >= 44px) */}
      <nav
        aria-label="Mobile Patient Navigation"
        className="md:hidden fixed bottom-0 left-0 right-0 z-40 bg-white/95 backdrop-blur-md border-t border-slate-200 shadow-lg px-2 py-1 flex items-center justify-around"
      >
        {patientNavItems.map((item) => {
          const isActive =
            item.to === "/patient/dashboard" ? pathname === item.to : pathname.startsWith(item.to);
          const Icon = item.icon;
          return (
            <Link
              key={item.to}
              to={item.to}
              className={cn(
                "flex flex-col items-center justify-center flex-1 py-1.5 min-h-[50px] min-w-[44px] rounded-xl text-[11px] font-semibold transition-colors",
                isActive ? "text-emerald-700 font-bold" : "text-slate-500 hover:text-slate-900",
              )}
            >
              <div
                className={cn(
                  "p-1 rounded-lg transition-colors",
                  isActive ? "bg-emerald-100 text-emerald-800" : "text-slate-500",
                )}
              >
                <Icon className="size-5" />
              </div>
              <span className="mt-0.5 leading-tight">{item.label}</span>
            </Link>
          );
        })}
      </nav>
    </div>
  );
}
