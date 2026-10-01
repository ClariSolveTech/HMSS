import { apiClient } from "@/lib/api-client";

export type BackendConsultationStatus = "IN_PROGRESS" | "COMPLETED" | "CANCELLED";

export interface CreateConsultationRequest {
  patientId: number;
  doctorId?: number | undefined;
  appointmentId?: number | undefined;
  queueEntryId?: number | undefined;
  chiefComplaint?: string | undefined;
  symptoms?: string | undefined;
  clinicalNotes?: string | undefined;
  diagnosis?: string | undefined;
  treatmentAdvice?: string | undefined;
}

export interface UpdateConsultationRequest {
  chiefComplaint?: string;
  symptoms?: string;
  clinicalNotes?: string;
  diagnosis?: string;
  treatmentAdvice?: string;
}

export interface ConsultationResponse {
  id: number;
  hospitalId: number;
  hospitalName?: string;
  doctorId: number;
  doctorName?: string;
  doctorSpecialization?: string;
  patientId: number;
  patientName?: string;
  patientCode?: string;
  appointmentId?: number | null;
  queueEntryId?: number | null;
  tokenNumber?: number | null;
  consultationDate: string;
  chiefComplaint?: string | null;
  symptoms?: string | null;
  clinicalNotes?: string | null;
  diagnosis?: string | null;
  treatmentAdvice?: string | null;
  status: BackendConsultationStatus;
  createdAt: string;
  updatedAt: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export const consultationApi = {
  createConsultation: (data: CreateConsultationRequest) =>
    apiClient.post<ConsultationResponse>("/api/v1/consultations", data),

  getConsultationById: (id: number) =>
    apiClient.get<ConsultationResponse>(`/api/v1/consultations/${id}`),

  getDoctorOwnConsultations: (page = 0, size = 20) =>
    apiClient.get<PaginatedResponse<ConsultationResponse>>(`/api/v1/doctors/me/consultations?page=${page}&size=${size}`),

  getPatientConsultations: (patientId: number, page = 0, size = 20) =>
    apiClient.get<PaginatedResponse<ConsultationResponse>>(`/api/v1/patients/${patientId}/consultations?page=${page}&size=${size}`),

  updateConsultation: (id: number, data: UpdateConsultationRequest) =>
    apiClient.put<ConsultationResponse>(`/api/v1/consultations/${id}`, data),

  completeConsultation: (id: number) =>
    apiClient.patch<ConsultationResponse>(`/api/v1/consultations/${id}/complete`),
};
