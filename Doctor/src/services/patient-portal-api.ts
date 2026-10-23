import { apiClient, ApiError } from "@/lib/api-client";

export function getPortalErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiError) {
    return error.message || fallback;
  }
  if (error instanceof Error) {
    return error.message || fallback;
  }
  return fallback;
}

export function isUnlinkedProfileError(error: unknown): boolean {
  if (error instanceof ApiError) {
    return (
      error.status === 403 || error.message?.toLowerCase().includes("no patient clinical profile")
    );
  }
  if (error instanceof Error) {
    return error.message?.toLowerCase().includes("no patient clinical profile");
  }
  return false;
}

export type AppointmentStatus =
  "BOOKED" | "CONFIRMED" | "CHECKED_IN" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED" | "NO_SHOW";

export type AppointmentType =
  "NEW_CONSULTATION" | "FOLLOW_UP" | "EMERGENCY" | "WALK_IN" | "TELECONSULTATION";

export interface PatientAppointmentSummary {
  id: number;
  appointmentNumber: string;
  doctorName: string;
  specialization: string;
  appointmentDate: string;
  startTime: string;
  endTime: string;
  status: AppointmentStatus;
  appointmentType: AppointmentType;
  isTeleconsultation: boolean;
  canJoinConsultation: boolean;
  reason?: string | undefined;
}

export interface PatientAppointmentDetailResponse {
  id: number;
  appointmentNumber: string;
  hospitalName: string;
  doctorName: string;
  specialization: string;
  appointmentDate: string;
  startTime: string;
  endTime: string;
  status: AppointmentStatus;
  appointmentType: AppointmentType;
  isTeleconsultation: boolean;
  canJoinConsultation: boolean;
  joinUrl?: string | null | undefined;
  eligibilityReason?: string | undefined;
  reason?: string | undefined;
  notes?: string | undefined;
  cancellationReason?: string | undefined;
  createdAt: string;
}

export interface TeleconsultAccessResponse {
  appointmentId: number;
  canJoin: boolean;
  joinUrl?: string | null | undefined;
  reason: string;
  scheduledDate: string;
  scheduledStartTime: string;
  scheduledEndTime: string;
}

export interface PatientDashboardSummaryResponse {
  patientCode: string;
  patientName: string;
  hospitalName: string;
  completedConsultationsCount: number;
  activePrescriptionsCount: number;
  labOrdersCount: number;
  nextAppointment?: PatientAppointmentSummary | null | undefined;
  recentAppointments: PatientAppointmentSummary[];
}

export interface PatientProfileResponse {
  id: number;
  patientCode: string;
  hospitalId: number;
  hospitalName: string;
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  gender: string;
  bloodGroup?: string | undefined;
  mobileNumber: string;
  email?: string | undefined;
  address?: string | undefined;
  city?: string | undefined;
  state?: string | undefined;
  pincode?: string | undefined;
  emergencyContactName?: string | undefined;
  emergencyContactNumber?: string | undefined;
  status: string;
  registeredAt: string;
}

export interface UpdatePatientProfileRequest {
  address?: string | undefined;
  city?: string | undefined;
  state?: string | undefined;
  pincode?: string | undefined;
  emergencyContactName?: string | undefined;
  emergencyContactNumber?: string | undefined;
}

export interface PatientPrescriptionSummaryResponse {
  id: number;
  prescriptionCode: string;
  doctorName: string;
  status: string;
  issuedDate: string;
  itemCount: number;
  notes?: string | undefined;
}

export interface PatientPrescriptionItemResponse {
  id: number;
  medicineName: string;
  dosage: string;
  frequency: string;
  duration: string;
  route?: string | undefined;
  instructions?: string | undefined;
}

export interface PatientPrescriptionDetailResponse {
  id: number;
  prescriptionCode: string;
  doctorName: string;
  status: string;
  issuedDate: string;
  notes?: string | undefined;
  items: PatientPrescriptionItemResponse[];
}

export interface PatientConsultationSummaryResponse {
  id: number;
  doctorName: string;
  consultationDate: string;
  chiefComplaint?: string | undefined;
  diagnosis?: string | undefined;
  treatmentAdvice?: string | undefined;
}

export interface PatientLabOrderSummaryResponse {
  id: number;
  orderNumber: string;
  doctorName: string;
  status: string;
  priority: string;
  orderedAt: string;
  testNames: string[];
}

export interface PatientRegisterRequest {
  firstName: string;
  lastName: string;
  dateOfBirth: string; // YYYY-MM-DD
  gender: "MALE" | "FEMALE" | "OTHER";
  mobileNumber: string;
  email?: string | undefined;
  password: string;
  hospitalId: number;
  address?: string | undefined;
  city?: string | undefined;
  state?: string | undefined;
  pincode?: string | undefined;
  emergencyContactName?: string | undefined;
  emergencyContactNumber?: string | undefined;
  bloodGroup?: string | undefined;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  userId: number;
  email?: string | undefined;
  firstName?: string | undefined;
  lastName?: string | undefined;
  hospitalId: number;
  roles: string[];
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface HospitalOption {
  id: number;
  name: string;
  code: string;
  city?: string | undefined;
  state?: string | undefined;
}

export interface PatientActivateRequest {
  activationCode: string;
  password: string;
  email?: string | undefined;
}

export const patientPortalApi = {
  // Authentication
  login: async (emailOrMobile: string, password: string): Promise<AuthResponse> => {
    return apiClient.post<AuthResponse>("/api/v1/auth/login", {
      emailOrMobile,
      password,
    });
  },

  register: async (data: PatientRegisterRequest): Promise<AuthResponse> => {
    return apiClient.post<AuthResponse>("/api/v1/auth/patient/register", data);
  },

  activate: async (data: PatientActivateRequest): Promise<AuthResponse> => {
    return apiClient.post<AuthResponse>("/api/v1/auth/patient/activate", data);
  },

  getAvailableHospitals: async (): Promise<HospitalOption[]> => {
    return apiClient.get<HospitalOption[]>("/api/v1/hospitals");
  },

  // Dashboard
  getDashboard: async (): Promise<PatientDashboardSummaryResponse> => {
    return apiClient.get<PatientDashboardSummaryResponse>("/api/v1/patient-portal/dashboard");
  },

  // Appointments
  getAppointments: async (params?: {
    status?: AppointmentStatus | undefined;
    appointmentType?: AppointmentType | undefined;
    page?: number | undefined;
    size?: number | undefined;
    sort?: string | undefined;
  }): Promise<PageResponse<PatientAppointmentSummary>> => {
    const query = new URLSearchParams();
    if (params?.status) query.append("status", params.status);
    if (params?.appointmentType) query.append("appointmentType", params.appointmentType);
    if (params?.page !== undefined) query.append("page", String(params.page));
    if (params?.size !== undefined) query.append("size", String(params.size));
    if (params?.sort) query.append("sort", params.sort);
    const qs = query.toString();
    return apiClient.get<PageResponse<PatientAppointmentSummary>>(
      `/api/v1/patient-portal/appointments${qs ? `?${qs}` : ""}`,
    );
  },

  getAppointmentDetail: async (id: number): Promise<PatientAppointmentDetailResponse> => {
    return apiClient.get<PatientAppointmentDetailResponse>(
      `/api/v1/patient-portal/appointments/${id}`,
    );
  },

  getTeleconsultAccess: async (id: number): Promise<TeleconsultAccessResponse> => {
    return apiClient.get<TeleconsultAccessResponse>(
      `/api/v1/patient-portal/appointments/${id}/teleconsult-access`,
    );
  },

  // Profile
  getProfile: async (): Promise<PatientProfileResponse> => {
    return apiClient.get<PatientProfileResponse>("/api/v1/patient-portal/profile");
  },

  updateProfile: async (data: UpdatePatientProfileRequest): Promise<PatientProfileResponse> => {
    return apiClient.put<PatientProfileResponse>("/api/v1/patient-portal/profile", data);
  },

  // Prescriptions
  getPrescriptions: async (params?: {
    page?: number | undefined;
    size?: number | undefined;
    sort?: string | undefined;
  }): Promise<PageResponse<PatientPrescriptionSummaryResponse>> => {
    const query = new URLSearchParams();
    if (params?.page !== undefined) query.append("page", String(params.page));
    if (params?.size !== undefined) query.append("size", String(params.size));
    if (params?.sort) query.append("sort", params.sort);
    const qs = query.toString();
    return apiClient.get<PageResponse<PatientPrescriptionSummaryResponse>>(
      `/api/v1/patient-portal/prescriptions${qs ? `?${qs}` : ""}`,
    );
  },

  getPrescriptionDetail: async (id: number): Promise<PatientPrescriptionDetailResponse> => {
    return apiClient.get<PatientPrescriptionDetailResponse>(
      `/api/v1/patient-portal/prescriptions/${id}`,
    );
  },

  // Health Records: Consultations
  getConsultations: async (params?: {
    page?: number | undefined;
    size?: number | undefined;
  }): Promise<PageResponse<PatientConsultationSummaryResponse>> => {
    const query = new URLSearchParams();
    if (params?.page !== undefined) query.append("page", String(params.page));
    if (params?.size !== undefined) query.append("size", String(params.size));
    const qs = query.toString();
    return apiClient.get<PageResponse<PatientConsultationSummaryResponse>>(
      `/api/v1/patient-portal/consultations${qs ? `?${qs}` : ""}`,
    );
  },

  // Health Records: Lab Orders
  getLabOrders: async (params?: {
    page?: number | undefined;
    size?: number | undefined;
    sort?: string | undefined;
  }): Promise<PageResponse<PatientLabOrderSummaryResponse>> => {
    const query = new URLSearchParams();
    if (params?.page !== undefined) query.append("page", String(params.page));
    if (params?.size !== undefined) query.append("size", String(params.size));
    if (params?.sort) query.append("sort", params.sort);
    const qs = query.toString();
    return apiClient.get<PageResponse<PatientLabOrderSummaryResponse>>(
      `/api/v1/patient-portal/lab-orders${qs ? `?${qs}` : ""}`,
    );
  },
};
