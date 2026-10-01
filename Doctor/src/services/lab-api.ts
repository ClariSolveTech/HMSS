import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/lib/api-client";

export type LabOrderStatus = "ORDERED" | "SAMPLE_COLLECTED" | "PROCESSING" | "COMPLETED" | "CANCELLED";
export type LabOrderPriority = "NORMAL" | "URGENT" | "STAT";
export type LabResultStatus = "PENDING" | "COMPLETED" | "REVIEWED";

export interface PaginatedLabResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first?: boolean | undefined;
  last?: boolean | undefined;
}

export interface LabTestResponse {
  id: number;
  hospitalId: number;
  testCode: string;
  testName: string;
  description?: string | undefined;
  category?: string | undefined;
  sampleType?: string | undefined;
  unit?: string | undefined;
  referenceRange?: string | undefined;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface LabOrderItemResponse {
  id: number;
  labOrderId: number;
  labTestId: number;
  testNameSnapshot: string;
  testCodeSnapshot: string;
  status: LabOrderStatus;
  notes?: string | undefined;
  createdAt: string;
  updatedAt: string;
}

export interface LabOrderResponse {
  id: number;
  orderNumber: string;
  hospitalId: number;
  patientId: number;
  patientName?: string | undefined;
  doctorId: number;
  doctorName?: string | undefined;
  consultationId?: number | undefined;
  status: LabOrderStatus;
  priority: LabOrderPriority;
  clinicalNotes?: string | undefined;
  orderedAt: string;
  completedAt?: string | undefined;
  cancelledAt?: string | undefined;
  createdAt: string;
  updatedAt: string;
  items: LabOrderItemResponse[];
}

export interface LabResultResponse {
  id: number;
  labOrderItemId: number;
  labOrderId: number;
  patientId: number;
  patientName?: string | undefined;
  doctorId: number;
  doctorName?: string | undefined;
  hospitalId: number;
  testNameSnapshot?: string | undefined;
  testCodeSnapshot?: string | undefined;
  resultValue?: string | undefined;
  resultUnit?: string | undefined;
  referenceRange?: string | undefined;
  abnormalFlag?: boolean | undefined;
  interpretation?: string | undefined;
  remarks?: string | undefined;
  resultStatus: LabResultStatus;
  resultedAt?: string | undefined;
  reviewedAt?: string | undefined;
  reviewedBy?: string | undefined;
  createdAt: string;
  updatedAt: string;
}

export interface CreateLabOrderItemRequest {
  labTestId: number;
  notes?: string | undefined;
}

export interface CreateLabOrderRequest {
  patientId: number;
  consultationId?: number | undefined;
  priority?: LabOrderPriority | undefined;
  clinicalNotes?: string | undefined;
  items: CreateLabOrderItemRequest[];
}

export interface UpdateLabOrderRequest {
  priority?: LabOrderPriority | undefined;
  clinicalNotes?: string | undefined;
}

export interface UpdateLabOrderStatusRequest {
  status: LabOrderStatus;
}

export interface ReviewLabResultRequest {
  interpretation?: string | undefined;
  remarks?: string | undefined;
}

/* API Service Object */

export const labApi = {
  // Lab Catalog
  getLabTests: () =>
    apiClient.get<LabTestResponse[]>("/api/v1/lab/tests"),

  getLabTestById: (id: number) =>
    apiClient.get<LabTestResponse>(`/api/v1/lab/tests/${id}`),

  // Lab Orders
  createLabOrder: (data: CreateLabOrderRequest) =>
    apiClient.post<LabOrderResponse>("/api/v1/lab/orders", data),

  getLabOrderById: (id: number) =>
    apiClient.get<LabOrderResponse>(`/api/v1/lab/orders/${id}`),

  getDoctorLabOrders: (page = 0, size = 20) =>
    apiClient.get<PaginatedLabResponse<LabOrderResponse>>(
      `/api/v1/doctors/me/lab-orders?page=${page}&size=${size}`
    ),

  getPatientLabOrders: (patientId: number, page = 0, size = 20) =>
    apiClient.get<PaginatedLabResponse<LabOrderResponse>>(
      `/api/v1/patients/${patientId}/lab-orders?page=${page}&size=${size}`
    ),

  updateLabOrder: (id: number, data: UpdateLabOrderRequest) =>
    apiClient.put<LabOrderResponse>(`/api/v1/lab/orders/${id}`, data),

  updateLabOrderStatus: (id: number, data: UpdateLabOrderStatusRequest) =>
    apiClient.patch<LabOrderResponse>(`/api/v1/lab/orders/${id}/status`, data),

  cancelLabOrder: (id: number) =>
    apiClient.patch<LabOrderResponse>(`/api/v1/lab/orders/${id}/cancel`),

  // Lab Results
  getLabResultById: (id: number) =>
    apiClient.get<LabResultResponse>(`/api/v1/lab/results/${id}`),

  getLabOrderResults: (orderId: number) =>
    apiClient.get<LabResultResponse[]>(`/api/v1/lab/orders/${orderId}/results`),

  getDoctorLabResults: (page = 0, size = 20) =>
    apiClient.get<PaginatedLabResponse<LabResultResponse>>(
      `/api/v1/doctors/me/lab-results?page=${page}&size=${size}`
    ),

  getPatientLabResults: (patientId: number, page = 0, size = 20) =>
    apiClient.get<PaginatedLabResponse<LabResultResponse>>(
      `/api/v1/patients/${patientId}/lab-results?page=${page}&size=${size}`
    ),

  reviewLabResult: (id: number, data?: ReviewLabResultRequest) =>
    apiClient.patch<LabResultResponse>(`/api/v1/lab/results/${id}/review`, data || {}),
};

/* React Query Hooks */

export function useLabTests() {
  return useQuery({
    queryKey: ["lab-tests"],
    queryFn: () => labApi.getLabTests(),
  });
}

export function useLabTest(id: number | null) {
  return useQuery({
    queryKey: ["lab-test", id],
    queryFn: () => (id ? labApi.getLabTestById(id) : null),
    enabled: Boolean(id),
  });
}

export function useCreateLabOrder() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateLabOrderRequest) => labApi.createLabOrder(data),
    onSuccess: (res) => {
      queryClient.invalidateQueries({ queryKey: ["doctor-lab-orders"] });
      if (res.patientId) {
        queryClient.invalidateQueries({ queryKey: ["patient-lab-orders", res.patientId] });
      }
    },
  });
}

export function useLabOrder(id: number | null) {
  return useQuery({
    queryKey: ["lab-order", id],
    queryFn: () => (id ? labApi.getLabOrderById(id) : null),
    enabled: Boolean(id),
  });
}

export function useDoctorLabOrders(page = 0, size = 20) {
  return useQuery({
    queryKey: ["doctor-lab-orders", page, size],
    queryFn: () => labApi.getDoctorLabOrders(page, size),
  });
}

export function usePatientLabOrders(patientId: number | null, page = 0, size = 20) {
  return useQuery({
    queryKey: ["patient-lab-orders", patientId, page, size],
    queryFn: () => (patientId ? labApi.getPatientLabOrders(patientId, page, size) : null),
    enabled: Boolean(patientId),
  });
}

export function useUpdateLabOrder() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateLabOrderRequest }) =>
      labApi.updateLabOrder(id, data),
    onSuccess: (res) => {
      queryClient.invalidateQueries({ queryKey: ["lab-order", res.id] });
      queryClient.invalidateQueries({ queryKey: ["doctor-lab-orders"] });
      if (res.patientId) {
        queryClient.invalidateQueries({ queryKey: ["patient-lab-orders", res.patientId] });
      }
    },
  });
}

export function useCancelLabOrder() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => labApi.cancelLabOrder(id),
    onSuccess: (res) => {
      queryClient.invalidateQueries({ queryKey: ["lab-order", res.id] });
      queryClient.invalidateQueries({ queryKey: ["doctor-lab-orders"] });
      if (res.patientId) {
        queryClient.invalidateQueries({ queryKey: ["patient-lab-orders", res.patientId] });
      }
    },
  });
}

export function useLabResult(id: number | null) {
  return useQuery({
    queryKey: ["lab-result", id],
    queryFn: () => (id ? labApi.getLabResultById(id) : null),
    enabled: Boolean(id),
  });
}

export function useLabOrderResults(orderId: number | null) {
  return useQuery({
    queryKey: ["lab-order-results", orderId],
    queryFn: () => (orderId ? labApi.getLabOrderResults(orderId) : []),
    enabled: Boolean(orderId),
  });
}

export function useDoctorLabResults(page = 0, size = 20) {
  return useQuery({
    queryKey: ["doctor-lab-results", page, size],
    queryFn: () => labApi.getDoctorLabResults(page, size),
  });
}

export function usePatientLabResults(patientId: number | null, page = 0, size = 20) {
  return useQuery({
    queryKey: ["patient-lab-results", patientId, page, size],
    queryFn: () => (patientId ? labApi.getPatientLabResults(patientId, page, size) : null),
    enabled: Boolean(patientId),
  });
}

export function useReviewLabResult() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data?: ReviewLabResultRequest | undefined }) =>
      labApi.reviewLabResult(id, data),
    onSuccess: (res) => {
      queryClient.invalidateQueries({ queryKey: ["lab-result", res.id] });
      queryClient.invalidateQueries({ queryKey: ["lab-order-results", res.labOrderId] });
      queryClient.invalidateQueries({ queryKey: ["doctor-lab-results"] });
      if (res.patientId) {
        queryClient.invalidateQueries({ queryKey: ["patient-lab-results", res.patientId] });
      }
    },
  });
}
