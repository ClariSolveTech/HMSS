import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/lib/api-client";
import { toast } from "sonner";

export type Gender = "MALE" | "FEMALE" | "OTHER";

export type BloodGroup =
  | "A_POSITIVE"
  | "A_NEGATIVE"
  | "B_POSITIVE"
  | "B_NEGATIVE"
  | "AB_POSITIVE"
  | "AB_NEGATIVE"
  | "O_POSITIVE"
  | "O_NEGATIVE";

export type PatientStatus = "ACTIVE" | "INACTIVE";

export interface CreatePatientRequest {
  firstName: string;
  lastName: string;
  dateOfBirth: string; // YYYY-MM-DD
  gender: Gender;
  mobileNumber: string;
  email?: string | undefined;
  address?: string | undefined;
  city?: string | undefined;
  state?: string | undefined;
  pincode?: string | undefined;
  emergencyContactName?: string | undefined;
  emergencyContactNumber?: string | undefined;
  bloodGroup?: BloodGroup | undefined;
}

export interface PatientResponse {
  id: number;
  userId?: number | null | undefined;
  patientCode: string;
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  gender: Gender;
  mobileNumber: string;
  email?: string | undefined;
  address?: string | undefined;
  city?: string | undefined;
  state?: string | undefined;
  pincode?: string | undefined;
  emergencyContactName?: string | undefined;
  emergencyContactNumber?: string | undefined;
  bloodGroup?: BloodGroup | undefined;
  status: PatientStatus;
  hospitalId: number;
  createdAt: string;
  updatedAt: string;
}

export interface PatientActivationCodeResponse {
  patientId: number;
  patientCode: string;
  patientName: string;
  mobileNumber: string;
  activationCode: string;
  expiresAt: string;
  instructions: string;
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

export interface GetPatientsParams {
  search?: string | undefined;
  page?: number | undefined;
  size?: number | undefined;
  sort?: string[] | undefined;
}

export const patientApi = {
  getPatients: (params?: GetPatientsParams) => {
    const query = new URLSearchParams();
    if (params?.search) query.set("search", params.search);
    if (params?.page !== undefined) query.set("page", String(params.page));
    if (params?.size !== undefined) query.set("size", String(params.size));
    if (params?.sort && params.sort.length > 0) query.set("sort", params.sort.join(","));
    const queryString = query.toString();
    return apiClient.get<SpringPage<PatientResponse>>(
      `/api/v1/patients${queryString ? `?${queryString}` : ""}`,
    );
  },

  getPatientById: (id: number) => apiClient.get<PatientResponse>(`/api/v1/patients/${id}`),

  createPatient: (data: CreatePatientRequest) =>
    apiClient.post<PatientResponse>("/api/v1/patients", data),

  generateActivationCode: (patientId: number) =>
    apiClient.post<PatientActivationCodeResponse>(
      `/api/v1/patients/${patientId}/generate-activation-code`,
    ),
};

/* React Query Hooks */

export function usePatients(params?: GetPatientsParams) {
  return useQuery({
    queryKey: ["patients", params?.search, params?.page, params?.size],
    queryFn: () => patientApi.getPatients(params),
  });
}

export function usePatient(id: number | null | undefined) {
  return useQuery({
    queryKey: ["patient", id],
    queryFn: () => patientApi.getPatientById(id!),
    enabled: Boolean(id),
  });
}

export function useCreatePatient() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreatePatientRequest) => patientApi.createPatient(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["patients"] });
      toast.success("Patient registered successfully!");
    },
    onError: (err: any) => {
      toast.error(err.message || "Failed to register patient");
    },
  });
}
