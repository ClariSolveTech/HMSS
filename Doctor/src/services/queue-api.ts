import { apiClient } from "@/lib/api-client";

export type BackendQueueStatus =
  | "WAITING"
  | "CALLED"
  | "IN_CONSULTATION"
  | "COMPLETED"
  | "SKIPPED"
  | "CANCELLED";

export type BackendQueuePriority = "NORMAL" | "URGENT" | "EMERGENCY";

export type BackendDoctorQueueStatus = "NOT_ARRIVED" | "ARRIVED" | "DONE";

export interface QueueEntryResponse {
  id: number;
  tokenNumber: number;
  hospitalId: number;
  doctorId: number;
  doctorName?: string;
  patientId: number;
  patientName?: string;
  patientCode?: string;
  appointmentId?: number | null;
  queueDate: string;
  priority: BackendQueuePriority;
  status: BackendQueueStatus;
  checkedInAt?: string;
  calledAt?: string | null;
  consultationStartedAt?: string | null;
  completedAt?: string | null;
  estimatedWaitMinutes?: number | null;
  actualWaitMinutes?: number | null;
  consultationDurationMinutes?: number | null;
  notes?: string | null;
  skipReason?: string | null;
  cancellationReason?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface CurrentTokenResponse {
  id: number;
  tokenNumber: number;
  patientName?: string;
  patientCode?: string;
  status: BackendQueueStatus;
  consultationStartedAt?: string | null;
}

export interface DoctorQueueStatusResponse {
  doctorId: number;
  doctorName?: string;
  status: BackendDoctorQueueStatus;
  updatedAt?: string;
}

export interface DoctorProfileResponse {
  id: number;
  doctorCode: string;
  specialization: string;
  qualification: string;
  licenseNumber: string;
  consultationFee: number;
  status: string;
  user: {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
    mobileNumber: string;
  };
  hospitalId: number;
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

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  userId: number;
  roles: string[];
  hospitalId: number;
}

export const queueApi = {
  login: (emailOrMobile: string, password: string) =>
    apiClient.post<LoginResponse>("/api/v1/auth/login", { emailOrMobile, password }),

  getDoctorProfileMe: () =>
    apiClient.get<DoctorProfileResponse>("/api/v1/doctors/me"),

  getDoctorOwnQueue: (date?: string, page = 0, size = 50) => {
    let url = `/api/v1/doctors/me/queue?page=${page}&size=${size}`;
    if (date) url += `&date=${date}`;
    return apiClient.get<SpringPage<QueueEntryResponse>>(url);
  },

  getDoctorOwnCurrentConsultation: () =>
    apiClient.get<CurrentTokenResponse | null>("/api/v1/doctors/me/queue/current"),

  callNextPatientForDoctor: () =>
    apiClient.post<QueueEntryResponse>("/api/v1/doctors/me/queue/next"),

  callPatient: (id: number) =>
    apiClient.patch<QueueEntryResponse>(`/api/v1/queue/${id}/call`),

  startConsultation: (id: number) =>
    apiClient.patch<QueueEntryResponse>(`/api/v1/queue/${id}/start-consultation`),

  completeConsultation: (id: number) =>
    apiClient.patch<QueueEntryResponse>(`/api/v1/queue/${id}/complete`),

  skipPatient: (id: number, reason = "Patient not present") =>
    apiClient.patch<QueueEntryResponse>(`/api/v1/queue/${id}/skip`, { reason }),

  recallPatient: (id: number) =>
    apiClient.patch<QueueEntryResponse>(`/api/v1/queue/${id}/recall`),

  cancelQueueEntry: (id: number, reason = "Cancelled") =>
    apiClient.patch<QueueEntryResponse>(`/api/v1/queue/${id}/cancel`, { reason }),

  updateDoctorOwnQueueStatus: (status: BackendDoctorQueueStatus) =>
    apiClient.patch<DoctorQueueStatusResponse>("/api/v1/doctors/me/queue/status", { status }),

  getDoctorQueueStatus: (doctorId: number) =>
    apiClient.get<DoctorQueueStatusResponse>(`/api/v1/doctors/${doctorId}/queue/status`),
};
