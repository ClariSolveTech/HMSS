import { apiClient } from "@/lib/api-client";
import { PaginatedResponse } from "./consultation-api";

export type PrescriptionStatus = "DRAFT" | "ISSUED" | "CANCELLED";

export interface PrescriptionItemRequest {
  medicineName: string;
  dosage: string;
  frequency: string;
  duration: string;
  route?: string;
  instructions?: string;
}

export interface CreatePrescriptionRequest {
  consultationId: number;
  notes?: string;
  items: PrescriptionItemRequest[];
}

export interface UpdatePrescriptionRequest {
  notes?: string;
  items?: PrescriptionItemRequest[];
}

export interface PrescriptionItemResponse {
  id: number;
  medicineName: string;
  dosage: string;
  frequency: string;
  duration: string;
  route?: string;
  instructions?: string;
}

export interface PrescriptionResponse {
  id: number;
  prescriptionCode: string;
  hospitalId: number;
  doctorId: number;
  patientId: number;
  consultationId: number;
  status: PrescriptionStatus;
  notes?: string;
  issuedAt?: string;
  createdAt: string;
  updatedAt: string;
  items: PrescriptionItemResponse[];
}

export const prescriptionApi = {
  createPrescription: (data: CreatePrescriptionRequest) =>
    apiClient.post<PrescriptionResponse>("/api/v1/prescriptions", data),

  getPrescriptionById: (id: number) =>
    apiClient.get<PrescriptionResponse>(`/api/v1/prescriptions/${id}`),

  updatePrescription: (id: number, data: UpdatePrescriptionRequest) =>
    apiClient.put<PrescriptionResponse>(`/api/v1/prescriptions/${id}`, data),

  issuePrescription: (id: number) =>
    apiClient.patch<PrescriptionResponse>(`/api/v1/prescriptions/${id}/issue`),

  cancelPrescription: (id: number) =>
    apiClient.patch<PrescriptionResponse>(`/api/v1/prescriptions/${id}/cancel`),

  getDoctorOwnPrescriptions: (page = 0, size = 20) =>
    apiClient.get<PaginatedResponse<PrescriptionResponse>>(
      `/api/v1/doctors/me/prescriptions?page=${page}&size=${size}`
    ),

  getPatientPrescriptions: (patientId: number, page = 0, size = 20) =>
    apiClient.get<PaginatedResponse<PrescriptionResponse>>(
      `/api/v1/patients/${patientId}/prescriptions?page=${page}&size=${size}`
    ),
};
