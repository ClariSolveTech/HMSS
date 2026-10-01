import type { ReactNode } from "react";
import {
  CalendarCheck,
  CalendarX,
  CalendarClock,
  UserCheck,
  Clock,
  Beaker,
  CheckCircle2,
  Pill,
  Receipt,
  Stethoscope,
  ListOrdered,
  Info,
} from "lucide-react";
import type { NotificationType, NotificationPriority } from "@/services/notifications-api";

export function getNotificationIcon(type: NotificationType): ReactNode {
  switch (type) {
    case "APPOINTMENT_CONFIRMED":
      return <CalendarCheck className="size-4 text-emerald-500 shrink-0" />;
    case "APPOINTMENT_CANCELLED":
      return <CalendarX className="size-4 text-rose-500 shrink-0" />;
    case "APPOINTMENT_RESCHEDULED":
      return <CalendarClock className="size-4 text-amber-500 shrink-0" />;
    case "PATIENT_CHECKED_IN":
      return <UserCheck className="size-4 text-sky-500 shrink-0" />;
    case "PATIENT_WAITING":
      return <Clock className="size-4 text-amber-500 shrink-0" />;
    case "LAB_RESULT_READY":
      return <Beaker className="size-4 text-purple-500 shrink-0" />;
    case "LAB_ORDER_COMPLETED":
      return <CheckCircle2 className="size-4 text-emerald-500 shrink-0" />;
    case "PRESCRIPTION_ISSUED":
      return <Pill className="size-4 text-blue-500 shrink-0" />;
    case "PAYMENT_RECEIVED":
      return <Receipt className="size-4 text-emerald-500 shrink-0" />;
    case "CONSULTATION_COMPLETED":
      return <Stethoscope className="size-4 text-primary shrink-0" />;
    case "QUEUE_UPDATE":
      return <ListOrdered className="size-4 text-indigo-500 shrink-0" />;
    case "SYSTEM":
    default:
      return <Info className="size-4 text-muted-foreground shrink-0" />;
  }
}

export function getPriorityBadgeClass(priority: NotificationPriority): string {
  switch (priority) {
    case "URGENT":
      return "bg-destructive/15 text-destructive border-destructive/30";
    case "HIGH":
      return "bg-amber-500/15 text-amber-700 dark:text-amber-400 border-amber-500/30";
    case "LOW":
      return "bg-muted text-muted-foreground border-border";
    case "NORMAL":
    default:
      return "bg-secondary text-secondary-foreground border-border";
  }
}

export function getReferenceRoute(referenceType?: string | null): string | null {
  if (!referenceType) return null;
  const upper = referenceType.toUpperCase();
  if (upper.includes("APPOINTMENT")) return "/appointments";
  if (upper.includes("LAB")) return "/laboratory";
  if (upper.includes("PRESCRIPTION")) return "/prescriptions";
  if (upper.includes("PAYMENT") || upper.includes("INVOICE")) return "/invoices";
  if (upper.includes("CONSULTATION")) return "/history";
  if (upper.includes("QUEUE")) return "/queue";
  return null;
}

export function formatNotificationTime(dateStr: string): string {
  if (!dateStr) return "";
  try {
    const date = new Date(dateStr);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffSec = Math.floor(diffMs / 1000);
    const diffMin = Math.floor(diffSec / 60);
    const diffHour = Math.floor(diffMin / 60);
    const diffDay = Math.floor(diffHour / 24);

    if (diffSec < 45) return "Just now";
    if (diffMin < 60) return `${diffMin}m ago`;
    if (diffHour < 24) return `${diffHour}h ago`;
    if (diffDay === 1) return "Yesterday";
    if (diffDay < 7) return `${diffDay}d ago`;

    return date.toLocaleDateString(undefined, {
      month: "short",
      day: "numeric",
      year: date.getFullYear() !== now.getFullYear() ? "numeric" : undefined,
    });
  } catch {
    return dateStr;
  }
}
