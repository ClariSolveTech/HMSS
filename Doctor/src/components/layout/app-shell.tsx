import { Link, useRouterState, useNavigate } from "@tanstack/react-router";
import { motion, AnimatePresence } from "motion/react";
import { useState, useEffect, type ReactNode } from "react";
import {
  Activity,
  Bell,
  Beaker,
  BarChart3,
  CalendarDays,
  ChevronDown,
  FileText,
  Search,
  Settings,
  Sparkles,
  Stethoscope,
  LayoutDashboard,
  ListOrdered,
  LogOut,
  Menu,
  Pill,
  Receipt,
  Users,
  X,
  Circle,
  CheckCircle2,
} from "lucide-react";

import { toast } from "sonner";

import { cn } from "@/lib/utils";
import { doctor, notifications } from "@/lib/healthcare-data";
import type { DoctorStatus } from "@/lib/healthcare-data";
import {
  getAuthUser,
  logout,
  isAuthenticated,
  isDoctor,
  isReceptionist,
  isOnlyReceptionist,
  isPatient,
  isStaff,
} from "@/lib/auth";
import { queueApi } from "@/services/queue-api";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { ScrollArea } from "@/components/ui/scroll-area";

import { NotificationDropdown } from "@/components/notifications/NotificationDropdown";

const doctorNav = [
  { to: "/", label: "Dashboard", icon: LayoutDashboard },
  { to: "/today", label: "Today's Patients", icon: Users },
  { to: "/appointments", label: "Appointments", icon: CalendarDays },
  { to: "/queue", label: "Current Queue", icon: ListOrdered },
  { to: "/notifications", label: "Notifications", icon: Bell },
  { to: "/patients", label: "Patient Search", icon: Search },
  { to: "/history", label: "Consultation History", icon: FileText },
  { to: "/prescriptions", label: "Prescriptions", icon: Pill },
  { to: "/invoices", label: "Billing & Invoices", icon: Receipt },
  { to: "/laboratory", label: "Laboratory", icon: Beaker },
  { to: "/reports", label: "Reports", icon: Activity },
  { to: "/analytics", label: "Analytics", icon: BarChart3 },
  { to: "/settings", label: "Settings", icon: Settings },
] as const;

const receptionistNav = [
  { to: "/receptionist", label: "Dashboard", icon: LayoutDashboard },
  { to: "/receptionist/patients", label: "Patients", icon: Users },
  { to: "/receptionist/appointments", label: "Appointments", icon: CalendarDays },
  { to: "/receptionist/check-in", label: "Check-In", icon: CheckCircle2 },
  { to: "/receptionist/queue", label: "Live Queue", icon: ListOrdered },
  { to: "/notifications", label: "Notifications", icon: Bell },
] as const;

const statusMeta: Record<DoctorStatus, { label: string; className: string }> = {
  available: { label: "Available", className: "text-success" },
  consulting: { label: "Consulting", className: "text-primary" },
  break: { label: "On Break", className: "text-warning" },
  lunch: { label: "Lunch", className: "text-warning" },
  emergency: { label: "Emergency", className: "text-destructive" },
  offline: { label: "Offline", className: "text-muted-foreground" },
};

function Brand() {
  const isRecep = isOnlyReceptionist();
  const homePath = isRecep ? "/receptionist" : "/";
  return (
    <Link to={homePath} className="flex items-center gap-2.5">
      <span className="brand-gradient grid size-9 shrink-0 place-items-center rounded-xl text-primary-foreground shadow-float">
        <Stethoscope className="size-5" />
      </span>
      <span className="min-w-0">
        <span className="block truncate text-[15px] font-extrabold leading-tight">Meridian</span>
        <span className="block truncate text-[11px] font-medium leading-tight text-muted-foreground">
          {isRecep ? "Reception OS" : "Health OS"}
        </span>
      </span>
    </Link>
  );
}

function NavList({ onNavigate }: { onNavigate?: () => void }) {
  const pathname = useRouterState({ select: (s) => s.location.pathname });
  const isRecep = isOnlyReceptionist();
  const items = isRecep ? receptionistNav : doctorNav;

  return (
    <nav className="flex flex-col gap-1 px-3 pb-6">
      {items.map((item) => {
        const active =
          item.to === "/" || item.to === "/receptionist"
            ? pathname === item.to
            : pathname.startsWith(item.to);
        return (
          <Link
            key={item.to}
            to={item.to}
            onClick={onNavigate}
            className={cn(
              "group relative flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-colors",
              active
                ? "text-sidebar-accent-foreground font-semibold"
                : "text-sidebar-foreground/75 hover:bg-sidebar-accent/60 hover:text-sidebar-accent-foreground",
            )}
          >
            {active && (
              <motion.span
                layoutId="nav-active"
                transition={{ type: "spring", stiffness: 400, damping: 34 }}
                className="absolute inset-0 rounded-xl bg-sidebar-accent"
              />
            )}
            <item.icon className="relative size-[18px] shrink-0" />
            <span className="relative truncate">{item.label}</span>
          </Link>
        );
      })}
    </nav>
  );
}

function NotificationBell() {
  const unread = notifications.filter((n) => n.unread).length;
  return (
    <Popover>
      <PopoverTrigger asChild>
        <Button variant="ghost" size="icon" className="relative rounded-xl">
          <Bell className="size-[18px]" />
          {unread > 0 && (
            <span className="absolute right-1.5 top-1.5 grid size-4 place-items-center rounded-full bg-destructive text-[9px] font-bold text-destructive-foreground">
              {unread}
            </span>
          )}
        </Button>
      </PopoverTrigger>
      <PopoverContent align="end" className="w-[min(22rem,calc(100vw-2rem))] p-0">
        <div className="flex items-center justify-between border-b px-4 py-3">
          <p className="text-sm font-semibold">Notifications</p>
          <span className="text-xs text-muted-foreground">{unread} unread</span>
        </div>
        <ScrollArea className="max-h-80">
          <ul className="divide-y">
            {notifications.map((n) => (
              <li key={n.id} className="flex gap-3 px-4 py-3 transition-colors hover:bg-muted/60">
                <span
                  className={cn(
                    "mt-1.5 size-2 shrink-0 rounded-full",
                    n.kind === "emergency"
                      ? "bg-destructive"
                      : n.kind === "lab"
                        ? "bg-info"
                        : n.kind === "reminder-failed"
                          ? "bg-warning"
                          : "bg-primary",
                  )}
                />
                <div className="min-w-0">
                  <p className="text-[13px] font-semibold">{n.title}</p>
                  <p className="text-xs text-muted-foreground">{n.body}</p>
                  <p className="mt-1 text-[11px] text-muted-foreground/80">{n.time}</p>
                </div>
              </li>
            ))}
          </ul>
        </ScrollArea>
      </PopoverContent>
    </Popover>
  );
}

export function AppShell({
  children,
  aiPanel,
}: {
  children: ReactNode;
  aiPanel?: ReactNode;
}) {
  const navigate = useNavigate();
  const pathname = useRouterState({ select: (s) => s.location.pathname });
  const [mobileOpen, setMobileOpen] = useState(false);
  const [status, setStatus] = useState<DoctorStatus>("consulting");
  const authUser = getAuthUser();
  const isRecep = isOnlyReceptionist();
  const doctorUser = isDoctor();
  const authenticated = isAuthenticated();

  useEffect(() => {
    if (!authenticated) {
      navigate({ to: "/login" });
      return;
    }

    if (isPatient() && !isStaff()) {
      toast.error("Unauthorized: Staff workspace is restricted to hospital personnel");
      navigate({ to: "/patient/dashboard" });
      return;
    }

    const isReceptionistRoute =
      pathname === "/receptionist" || pathname.startsWith("/receptionist/");
    const isSharedRoute = pathname === "/notifications";

    if (isRecep && !isReceptionistRoute && !isSharedRoute) {
      toast.error("Unauthorized: Doctor clinical workspace is restricted");
      navigate({ to: "/receptionist" });
    } else if (doctorUser && !isReceptionist() && isReceptionistRoute) {
      toast.error("Unauthorized: Receptionist front desk is restricted");
      navigate({ to: "/" });
    }
  }, [authenticated, isRecep, doctorUser, pathname, navigate]);

  const handleStatusChange = async (s: DoctorStatus) => {
    setStatus(s);
    try {
      let backendStatus: "NOT_ARRIVED" | "ARRIVED" | "DONE" = "ARRIVED";
      if (s === "offline" || s === "break" || s === "lunch") {
        backendStatus = "DONE";
      } else if (s === "available" || s === "consulting" || s === "emergency") {
        backendStatus = "ARRIVED";
      }
      await queueApi.updateDoctorOwnQueueStatus(backendStatus);
      toast.success(`Doctor status updated to ${s}`);
    } catch {
      // Ignored if doctor profile not registered yet
    }
  };

  const displayName = isRecep
    ? authUser?.firstName
      ? `${authUser.firstName} ${authUser.lastName || ""}`.trim()
      : "Reception Desk"
    : authUser?.email
      ? authUser.email.split("@")[0]?.replace(".", " ") ?? doctor.name
      : doctor.name;

  const roleSubtitle = isRecep ? "Receptionist · CCH001" : doctor.specialty;

  return (
    <div className="min-h-screen bg-background">
      <header className="glass-panel sticky top-0 z-40 flex h-16 items-center gap-3 rounded-none border-x-0 border-t-0 px-3 sm:px-5">
        <Button
          variant="ghost"
          size="icon"
          className="rounded-xl lg:hidden"
          onClick={() => setMobileOpen(true)}
          aria-label="Open navigation"
        >
          <Menu className="size-5" />
        </Button>
        <div className="hidden w-[236px] shrink-0 lg:block">
          <Brand />
        </div>
        <div className="lg:hidden">
          <Brand />
        </div>

        <div className="ml-auto flex items-center gap-1 sm:gap-2">
          {!isRecep && (
            <>
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button variant="outline" size="sm" className="hidden rounded-xl md:flex">
                    <Circle className={cn("size-2.5 fill-current", statusMeta[status].className)} />
                    <span className="text-xs font-semibold">{statusMeta[status].label}</span>
                    <ChevronDown className="size-3.5 opacity-60" />
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  <DropdownMenuLabel>Doctor status</DropdownMenuLabel>
                  <DropdownMenuSeparator />
                  {(Object.keys(statusMeta) as DoctorStatus[]).map((s) => (
                    <DropdownMenuItem key={s} onClick={() => handleStatusChange(s)}>
                      <Circle className={cn("size-2.5 fill-current", statusMeta[s].className)} />
                      {statusMeta[s].label}
                    </DropdownMenuItem>
                  ))}
                </DropdownMenuContent>
              </DropdownMenu>

              <Button variant="ghost" size="icon" className="rounded-xl text-primary">
                <Sparkles className="size-[18px]" />
                <span className="sr-only">AI Assistant</span>
              </Button>
            </>
          )}

          {isRecep && (
            <span className="hidden sm:inline-flex items-center gap-1.5 px-3 py-1 rounded-xl text-xs font-semibold bg-muted text-muted-foreground border">
              City Central Hospital (CCH001)
            </span>
          )}

          <NotificationDropdown />
          {!isRecep && (
            <Link to="/settings" className="hidden sm:block">
              <Button variant="ghost" size="icon" className="rounded-xl">
                <Settings className="size-[18px]" />
              </Button>
            </Link>
          )}

          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <button className="flex items-center gap-2 rounded-xl py-1 pl-1 pr-2 transition-colors hover:bg-muted">
                {isRecep ? (
                  <div className="size-9 rounded-lg border bg-primary/10 text-primary grid place-items-center font-bold text-xs shrink-0">
                    RD
                  </div>
                ) : (
                  <img
                    src={doctor.photo}
                    alt={displayName}
                    className="size-9 rounded-lg border object-cover"
                  />
                )}
                <span className="hidden min-w-0 text-left sm:block">
                  <span className="block truncate text-[13px] font-semibold leading-tight capitalize">
                    {displayName}
                  </span>
                  <span className="block truncate text-[11px] leading-tight text-muted-foreground">
                    {roleSubtitle}
                  </span>
                </span>
              </button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-56">
              <DropdownMenuLabel className="font-normal">
                <p className="text-sm font-semibold capitalize">{displayName}</p>
                <p className="text-xs text-muted-foreground">{authUser?.email || doctor.regNo}</p>
              </DropdownMenuLabel>
              <DropdownMenuSeparator />
              {!isRecep && (
                <DropdownMenuItem asChild>
                  <Link to="/settings">
                    <Settings className="size-4" /> Settings
                  </Link>
                </DropdownMenuItem>
              )}
              <DropdownMenuItem
                className="text-destructive focus:text-destructive cursor-pointer"
                onClick={() => logout()}
              >
                <LogOut className="size-4 mr-1" /> Logout
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </header>

      <div className="flex">
        <aside className="sticky top-16 hidden h-[calc(100vh-4rem)] w-[252px] shrink-0 overflow-y-auto border-r bg-sidebar pt-4 scroll-slim lg:block">
          <NavList />
        </aside>

        <AnimatePresence>
          {mobileOpen && (
            <>
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                onClick={() => setMobileOpen(false)}
                className="fixed inset-0 z-50 bg-foreground/40 backdrop-blur-sm lg:hidden"
              />
              <motion.aside
                initial={{ x: "-100%" }}
                animate={{ x: 0 }}
                exit={{ x: "-100%" }}
                transition={{ type: "spring", stiffness: 380, damping: 36 }}
                className="fixed inset-y-0 left-0 z-50 w-[272px] overflow-y-auto bg-sidebar pt-4 scroll-slim lg:hidden"
              >
                <div className="mb-4 flex items-center justify-between px-4">
                  <Brand />
                  <Button variant="ghost" size="icon" onClick={() => setMobileOpen(false)}>
                    <X className="size-5" />
                  </Button>
                </div>
                <NavList onNavigate={() => setMobileOpen(false)} />
              </motion.aside>
            </>
          )}
        </AnimatePresence>

        <main className="min-w-0 flex-1">{children}</main>
        {aiPanel}
      </div>
    </div>
  );
}

export function PageHeader({
  title,
  subtitle,
  actions,
}: {
  title: string;
  subtitle?: string;
  actions?: ReactNode;
}) {
  return (
    <div className="grid grid-cols-[minmax(0,1fr)_auto] items-center gap-4 sm:flex sm:flex-wrap sm:justify-between">
      <div className="min-w-0">
        <h1 className="truncate text-xl font-extrabold sm:text-2xl">{title}</h1>
        {subtitle && <p className="mt-1 text-sm text-muted-foreground">{subtitle}</p>}
      </div>
      {actions && <div className="flex shrink-0 items-center gap-2">{actions}</div>}
    </div>
  );
}
