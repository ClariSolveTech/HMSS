import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/lib/api-client";
import { toast } from "sonner";

export type AppointmentStatus =
  | "BOOKED"
  | "CONFIRMED"
  | "CHECKED_IN"
  | "IN_PROGRESS"
  | "COMPLETED"
  | "CANCELLED"
  | "NO_SHOW";

export type AppointmentType =
  | "NEW_CONSULTATION"
  | "FOLLOW_UP"
  | "EMERGENCY"
  | "WALK_IN";

export type BookingSource =
  | "RECEPTION"
  | "PATIENT_PORTAL"
  | "DOCTOR"
  | "WALK_IN";

export interface AppointmentResponse {
  id: number;
  appointmentNumber: string;
  hospitalId: number;
  patientId: number;
  patientName: string;
  doctorId: number;
  doctorName: string;
  specialization?: string | undefined;
  appointmentDate: string;
  startTime: string;
  endTime: string;
  status: AppointmentStatus;
  appointmentType: AppointmentType;
  bookingSource: BookingSource;
  reason?: string | undefined;
  notes?: string | undefined;
  cancellationReason?: string | undefined;
  cancelledAt?: string | undefined;
  createdAt: string;
  updatedAt: string;
}

export interface CreateAppointmentRequest {
  patientId: number;
  doctorId: number;
  appointmentDate: string;
  startTime: string;
  appointmentType?: AppointmentType | undefined;
  bookingSource?: BookingSource | undefined;
  reason?: string | undefined;
  notes?: string | undefined;
}

export interface RescheduleAppointmentRequest {
  appointmentDate: string;
  startTime: string;
}

export interface CancelAppointmentRequest {
  reason?: string | undefined;
}

export interface GetAppointmentsParams {
  doctorId?: number | undefined;
  patientId?: number | undefined;
  appointmentDate?: string | undefined;
  status?: AppointmentStatus | undefined;
  appointmentType?: AppointmentType | undefined;
  search?: string | undefined;
  page?: number | undefined;
  size?: number | undefined;
  sort?: string[] | undefined;
}

export interface SpringPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export const appointmentApi = {
  createAppointment: (data: CreateAppointmentRequest) =>
    apiClient.post<AppointmentResponse>("/api/v1/appointments", data),

  getAppointmentById: (id: number) =>
    apiClient.get<AppointmentResponse>(`/api/v1/appointments/${id}`),

  getAppointments: (params?: GetAppointmentsParams) => {
    const query = new URLSearchParams();
    if (params?.doctorId !== undefined) query.set("doctorId", String(params.doctorId));
    if (params?.patientId !== undefined) query.set("patientId", String(params.patientId));
    if (params?.appointmentDate) query.set("appointmentDate", params.appointmentDate);
    if (params?.status) query.set("status", params.status);
    if (params?.appointmentType) query.set("appointmentType", params.appointmentType);
    if (params?.search) query.set("search", params.search);
    if (params?.page !== undefined) query.set("page", String(params.page));
    if (params?.size !== undefined) query.set("size", String(params.size));
    if (params?.sort && params.sort.length > 0) query.set("sort", params.sort.join(","));
    const queryString = query.toString();
    return apiClient.get<SpringPage<AppointmentResponse>>(`/api/v1/appointments${queryString ? `?${queryString}` : ""}`);
  },

  getTodayAppointments: (doctorId?: number, page = 0, size = 20) => {
    let url = `/api/v1/appointments/today?page=${page}&size=${size}`;
    if (doctorId !== undefined) url += `&doctorId=${doctorId}`;
    return apiClient.get<SpringPage<AppointmentResponse>>(url);
  },

  getDoctorAppointments: (
    doctorId: number,
    date?: string,
    status?: AppointmentStatus,
    appointmentType?: AppointmentType,
    search?: string,
    page = 0,
    size = 20
  ) => {
    let url = `/api/v1/doctors/${doctorId}/appointments?page=${page}&size=${size}`;
    if (date) url += `&date=${date}`;
    if (status) url += `&status=${status}`;
    if (appointmentType) url += `&appointmentType=${appointmentType}`;
    if (search) url += `&search=${encodeURIComponent(search)}`;
    return apiClient.get<SpringPage<AppointmentResponse>>(url);
  },

  getPatientAppointments: (patientId: number, page = 0, size = 20) =>
    apiClient.get<SpringPage<AppointmentResponse>>(`/api/v1/patients/${patientId}/appointments?page=${page}&size=${size}`),

  confirmAppointment: (id: number) =>
    apiClient.patch<AppointmentResponse>(`/api/v1/appointments/${id}/confirm`),

  checkInAppointment: (id: number) =>
    apiClient.patch<AppointmentResponse>(`/api/v1/appointments/${id}/check-in`),

  markNoShowAppointment: (id: number) =>
    apiClient.patch<AppointmentResponse>(`/api/v1/appointments/${id}/no-show`),

  cancelAppointment: (id: number, reason?: string) =>
    apiClient.patch<AppointmentResponse>(`/api/v1/appointments/${id}/cancel`, reason ? { reason } : {}),

  rescheduleAppointment: (id: number, data: RescheduleAppointmentRequest) =>
    apiClient.patch<AppointmentResponse>(`/api/v1/appointments/${id}/reschedule`, data),
};

/* React Query Hooks */

export function useAppointments(params?: GetAppointmentsParams) {
  return useQuery({
    queryKey: ["appointments", params?.doctorId, params?.patientId, params?.appointmentDate, params?.status, params?.appointmentType, params?.search, params?.page, params?.size],
    queryFn: () => appointmentApi.getAppointments(params),
  });
}

export function useDoctorAppointments(
  doctorId: number,
  date?: string,
  status?: AppointmentStatus,
  appointmentType?: AppointmentType,
  search?: string,
  page = 0,
  size = 20
) {
  return useQuery({
    queryKey: ["doctor-appointments", doctorId, date, status, appointmentType, search, page, size],
    queryFn: () => appointmentApi.getDoctorAppointments(doctorId, date, status, appointmentType, search, page, size),
    enabled: Boolean(doctorId),
  });
}

export function useTodayAppointments(doctorId?: number, page = 0, size = 20) {
  return useQuery({
    queryKey: ["today-appointments", doctorId, page, size],
    queryFn: () => appointmentApi.getTodayAppointments(doctorId, page, size),
  });
}

export function useAppointment(id: number | null) {
  return useQuery({
    queryKey: ["appointment", id],
    queryFn: () => (id ? appointmentApi.getAppointmentById(id) : null),
    enabled: Boolean(id),
  });
}

export function useConfirmAppointment() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => appointmentApi.confirmAppointment(id),
    onSuccess: (res) => {
      toast.success(`Appointment #${res.appointmentNumber} confirmed`);
      queryClient.invalidateQueries({ queryKey: ["appointments"] });
      queryClient.invalidateQueries({ queryKey: ["doctor-appointments"] });
      queryClient.invalidateQueries({ queryKey: ["today-appointments"] });
      queryClient.invalidateQueries({ queryKey: ["appointment", res.id] });
    },
    onError: (err: any) => {
      toast.error(err.message || "Failed to confirm appointment");
    },
  });
}

export function useCheckInAppointment() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => appointmentApi.checkInAppointment(id),
    onSuccess: (res) => {
      toast.success(`Appointment #${res.appointmentNumber} checked in`);
      queryClient.invalidateQueries({ queryKey: ["appointments"] });
      queryClient.invalidateQueries({ queryKey: ["doctor-appointments"] });
      queryClient.invalidateQueries({ queryKey: ["today-appointments"] });
      queryClient.invalidateQueries({ queryKey: ["appointment", res.id] });
      queryClient.invalidateQueries({ queryKey: ["doctor-queue"] });
      queryClient.invalidateQueries({ queryKey: ["doctor-current-token"] });
    },
    onError: (err: any) => {
      toast.error(err.message || "Failed to check in appointment");
    },
  });
}

export function useMarkNoShow() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => appointmentApi.markNoShowAppointment(id),
    onSuccess: (res) => {
      toast.success(`Appointment #${res.appointmentNumber} marked as NO-SHOW`);
      queryClient.invalidateQueries({ queryKey: ["appointments"] });
      queryClient.invalidateQueries({ queryKey: ["doctor-appointments"] });
      queryClient.invalidateQueries({ queryKey: ["today-appointments"] });
      queryClient.invalidateQueries({ queryKey: ["appointment", res.id] });
    },
    onError: (err: any) => {
      toast.error(err.message || "Failed to mark appointment as no-show");
    },
  });
}

export function useCancelAppointment() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, reason }: { id: number; reason?: string }) =>
      appointmentApi.cancelAppointment(id, reason),
    onSuccess: (res) => {
      toast.success(`Appointment #${res.appointmentNumber} cancelled`);
      queryClient.invalidateQueries({ queryKey: ["appointments"] });
      queryClient.invalidateQueries({ queryKey: ["doctor-appointments"] });
      queryClient.invalidateQueries({ queryKey: ["today-appointments"] });
      queryClient.invalidateQueries({ queryKey: ["appointment", res.id] });
    },
    onError: (err: any) => {
      toast.error(err.message || "Failed to cancel appointment");
    },
  });
}

export function useRescheduleAppointment() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: RescheduleAppointmentRequest }) =>
      appointmentApi.rescheduleAppointment(id, data),
    onSuccess: (res) => {
      toast.success(`Appointment #${res.appointmentNumber} rescheduled`);
      queryClient.invalidateQueries({ queryKey: ["appointments"] });
      queryClient.invalidateQueries({ queryKey: ["doctor-appointments"] });
      queryClient.invalidateQueries({ queryKey: ["today-appointments"] });
      queryClient.invalidateQueries({ queryKey: ["appointment", res.id] });
    },
    onError: (err: any) => {
      toast.error(err.message || "Failed to reschedule appointment");
    },
  });
}
