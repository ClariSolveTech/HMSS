import { apiClient } from "@/lib/api-client";
import { PaginatedResponse } from "./consultation-api";

export type InvoiceStatus = "DRAFT" | "ISSUED" | "PARTIALLY_PAID" | "PAID" | "CANCELLED";
export type PaymentStatus = "PENDING" | "SUCCESS" | "FAILED" | "REFUNDED";
export type PaymentMethod = "CASH" | "UPI" | "CARD" | "NET_BANKING" | "OTHER";
export type InvoiceItemType = "CONSULTATION" | "MEDICINE" | "LAB" | "PROCEDURE" | "OTHER";

export interface InvoiceItemRequest {
  itemType: InvoiceItemType;
  description: string;
  quantity: number;
  unitPrice: number;
  discount?: number | undefined;
  tax?: number | undefined;
}

export interface CreateInvoiceRequest {
  patientId: number;
  doctorId: number;
  appointmentId?: number | undefined;
  consultationId?: number | undefined;
  notes?: string | undefined;
  items: InvoiceItemRequest[];
}

export interface UpdateInvoiceRequest {
  notes?: string | undefined;
  items: InvoiceItemRequest[];
}

export interface CreatePaymentRequest {
  amount: number;
  paymentMethod: PaymentMethod;
  transactionReference?: string | undefined;
  notes?: string | undefined;
}

export interface InvoiceItemResponse {
  id: number;
  itemType: InvoiceItemType;
  description: string;
  quantity: number;
  unitPrice: number;
  discount: number;
  tax: number;
  lineTotal: number;
}

export interface PaymentResponse {
  id: number;
  paymentReference: string;
  amount: number;
  paymentMethod: PaymentMethod;
  status: PaymentStatus;
  transactionReference?: string | undefined;
  notes?: string | undefined;
  paidAt?: string | undefined;
  createdAt: string;
}

export interface InvoiceResponse {
  id: number;
  invoiceNumber: string;
  hospitalId: number;
  patientId: number;
  doctorId: number;
  appointmentId?: number | null | undefined;
  consultationId?: number | null | undefined;
  subtotal: number;
  discount: number;
  tax: number;
  totalAmount: number;
  paidAmount: number;
  dueAmount: number;
  status: InvoiceStatus;
  notes?: string | undefined;
  issuedAt?: string | undefined;
  createdAt: string;
  updatedAt: string;
  items: InvoiceItemResponse[];
  payments: PaymentResponse[];
}

export const billingApi = {
  createInvoice: (data: CreateInvoiceRequest) =>
    apiClient.post<InvoiceResponse>("/api/v1/invoices", data),

  getInvoiceById: (id: number) =>
    apiClient.get<InvoiceResponse>(`/api/v1/invoices/${id}`),

  updateInvoice: (id: number, data: UpdateInvoiceRequest) =>
    apiClient.put<InvoiceResponse>(`/api/v1/invoices/${id}`, data),

  issueInvoice: (id: number) =>
    apiClient.patch<InvoiceResponse>(`/api/v1/invoices/${id}/issue`),

  cancelInvoice: (id: number) =>
    apiClient.patch<InvoiceResponse>(`/api/v1/invoices/${id}/cancel`),

  addPayment: (id: number, data: CreatePaymentRequest) =>
    apiClient.post<PaymentResponse>(`/api/v1/invoices/${id}/payments`, data),

  getPaymentsByInvoiceId: (id: number) =>
    apiClient.get<PaymentResponse[]>(`/api/v1/invoices/${id}/payments`),

  getPatientInvoices: (patientId: number, page = 0, size = 20) =>
    apiClient.get<PaginatedResponse<InvoiceResponse>>(
      `/api/v1/patients/${patientId}/invoices?page=${page}&size=${size}`
    ),

  getDoctorOwnInvoices: (page = 0, size = 20) =>
    apiClient.get<PaginatedResponse<InvoiceResponse>>(
      `/api/v1/doctors/me/invoices?page=${page}&size=${size}`
    ),
};
