import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/lib/api-client";

export interface DoctorUser {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  mobileNumber: string;
}

export interface DoctorResponse {
  id: number;
  doctorCode: string;
  specialization: string;
  qualification: string;
  licenseNumber: string;
  consultationFee: number;
  status: string;
  queueStatus: string;
  user?: DoctorUser;
  userId?: number;
  firstName?: string;
  lastName?: string;
  email?: string;
  mobileNumber?: string;
  hospitalId: number;
}

export function getDoctorFullName(doc?: Partial<DoctorResponse> | null): string {
  if (!doc) return "Doctor";
  const first = doc.firstName || doc.user?.firstName || "";
  const last = doc.lastName || doc.user?.lastName || "";
  const full = `${first} ${last}`.trim();
  if (full) return full;
  if (doc.doctorCode) return doc.doctorCode;
  if (doc.id) return `Doctor #${doc.id}`;
  return "Doctor";
}

export interface AvailableSlotDto {
  startTime: string;
  endTime: string;
  available: boolean;
}

export interface DoctorAvailabilityResponse {
  doctorId: number;
  date: string;
  slots: AvailableSlotDto[];
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

export const doctorApi = {
  getDoctors: (specialization?: string, status?: string, page = 0, size = 50) => {
    let url = `/api/v1/doctors?page=${page}&size=${size}`;
    if (specialization) url += `&specialization=${encodeURIComponent(specialization)}`;
    if (status) url += `&status=${encodeURIComponent(status)}`;
    return apiClient.get<SpringPage<DoctorResponse>>(url);
  },

  getDoctorById: (id: number) =>
    apiClient.get<DoctorResponse>(`/api/v1/doctors/${id}`),

  getDoctorAvailability: (doctorId: number, date: string) =>
    apiClient.get<DoctorAvailabilityResponse>(`/api/v1/doctors/${doctorId}/availability?date=${date}`),
};

export function useDoctors(specialization?: string, status?: string) {
  return useQuery({
    queryKey: ["doctors", specialization, status],
    queryFn: () => doctorApi.getDoctors(specialization, status),
  });
}

export function useDoctorAvailability(doctorId: number | null | undefined, date: string | null | undefined) {
  return useQuery({
    queryKey: ["doctor-availability", doctorId, date],
    queryFn: () => doctorApi.getDoctorAvailability(doctorId!, date!),
    enabled: Boolean(doctorId && date),
  });
}
