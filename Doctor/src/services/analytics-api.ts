import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/lib/api-client";
import { isDoctor } from "@/lib/auth";

/* ─── Types ───────────────────────────────────────────────────────────────── */

export interface ConsultationOutcomeSummary {
  status: string;
  count: number;
}

export interface DoctorDashboardSummary {
  todayAppointmentsCount: number;
  todayConsultationsCount: number;
  todayPatientsCount: number;
  completedConsultationsCount: number;
  noShowAppointmentsCount: number;
  cancelledAppointmentsCount: number;
  waitingQueueCount: number;
  inConsultationQueueCount: number;
  completedQueueCount: number;
  prescriptionsIssuedCount: number;
  labOrdersCount: number;
  completedLabOrdersCount: number;
  pendingLabOrdersCount: number;
  totalRevenueBilled: number;
  totalRevenueCollected: number;
  outcomesSummary: ConsultationOutcomeSummary[];
}

export interface DailyTrendPoint {
  date: string;
  appointments: number;
  consultations: number;
  patients: number;
  revenue: number;
}

/* ─── API calls ────────────────────────────────────────────────────────────── */

export const analyticsApi = {
  getSummary: () =>
    apiClient.get<DoctorDashboardSummary>("/api/v1/doctors/me/analytics/summary"),

  getTrends: (from?: string, to?: string) => {
    const params = new URLSearchParams();
    if (from) params.append("from", from);
    if (to) params.append("to", to);
    const qs = params.toString();
    return apiClient.get<DailyTrendPoint[]>(
      `/api/v1/doctors/me/analytics/trends${qs ? `?${qs}` : ""}`
    );
  },
};

/* ─── React Query hooks ────────────────────────────────────────────────────── */

export function useDoctorAnalyticsSummary() {
  return useQuery({
    queryKey: ["doctor-analytics-summary"],
    queryFn: () => analyticsApi.getSummary(),
    enabled: isDoctor(),
    staleTime: 60_000, // 1 minute
    retry: 1,
  });
}

export function useDoctorAnalyticsTrends(from?: string, to?: string) {
  return useQuery({
    queryKey: ["doctor-analytics-trends", from, to],
    queryFn: () => analyticsApi.getTrends(from, to),
    enabled: isDoctor(),
    staleTime: 60_000, // 1 minute
    retry: 1,
  });
}
