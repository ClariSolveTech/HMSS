import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/lib/api-client";
import type { SpringPage } from "./queue-api";

export type NotificationStatus = "UNREAD" | "READ";

export type NotificationPriority = "LOW" | "NORMAL" | "HIGH" | "URGENT";

export type NotificationType =
  | "APPOINTMENT_CONFIRMED"
  | "APPOINTMENT_CANCELLED"
  | "APPOINTMENT_RESCHEDULED"
  | "PATIENT_CHECKED_IN"
  | "PATIENT_WAITING"
  | "LAB_RESULT_READY"
  | "LAB_ORDER_COMPLETED"
  | "PRESCRIPTION_ISSUED"
  | "PAYMENT_RECEIVED"
  | "CONSULTATION_COMPLETED"
  | "QUEUE_UPDATE"
  | "SYSTEM";

export interface NotificationResponse {
  id: number;
  type: NotificationType;
  title: string;
  message: string;
  status: NotificationStatus;
  priority: NotificationPriority;
  referenceType?: string | null;
  referenceId?: number | null;
  createdAt: string;
  readAt?: string | null;
}

export interface UnreadCountResponse {
  unreadCount: number;
  count: number;
}

// API functions
export const notificationsApi = {
  getNotifications: (page = 0, size = 20) =>
    apiClient.get<SpringPage<NotificationResponse>>(`/api/v1/notifications?page=${page}&size=${size}`),

  getUnreadNotifications: (page = 0, size = 20) =>
    apiClient.get<SpringPage<NotificationResponse>>(`/api/v1/notifications/unread?page=${page}&size=${size}`),

  getUnreadNotificationCount: () =>
    apiClient.get<UnreadCountResponse>("/api/v1/notifications/unread-count"),

  getNotification: (id: number) =>
    apiClient.get<NotificationResponse>(`/api/v1/notifications/${id}`),

  markNotificationAsRead: (id: number) =>
    apiClient.patch<NotificationResponse>(`/api/v1/notifications/${id}/read`),

  markAllNotificationsAsRead: () =>
    apiClient.patch<{ message: string }>("/api/v1/notifications/read-all"),

  deleteNotification: (id: number) =>
    apiClient.delete<void>(`/api/v1/notifications/${id}`),
};

// React Query Hooks
export const NOTIFICATION_QUERY_KEYS = {
  all: ["notifications"] as const,
  list: (page = 0, size = 20) => ["notifications", "list", page, size] as const,
  unreadList: (page = 0, size = 20) => ["notifications", "unread", page, size] as const,
  unreadCount: ["notifications", "unread-count"] as const,
  detail: (id: number) => ["notifications", "detail", id] as const,
};

export function useNotifications(page = 0, size = 20) {
  return useQuery({
    queryKey: NOTIFICATION_QUERY_KEYS.list(page, size),
    queryFn: () => notificationsApi.getNotifications(page, size),
    staleTime: 30000,
  });
}

export function useUnreadNotifications(page = 0, size = 20) {
  return useQuery({
    queryKey: NOTIFICATION_QUERY_KEYS.unreadList(page, size),
    queryFn: () => notificationsApi.getUnreadNotifications(page, size),
    staleTime: 30000,
  });
}

export function useUnreadNotificationCount() {
  return useQuery({
    queryKey: NOTIFICATION_QUERY_KEYS.unreadCount,
    queryFn: () => notificationsApi.getUnreadNotificationCount(),
    staleTime: 30000,
  });
}

export function useNotification(id: number) {
  return useQuery({
    queryKey: NOTIFICATION_QUERY_KEYS.detail(id),
    queryFn: () => notificationsApi.getNotification(id),
    enabled: !!id,
  });
}

export function useMarkNotificationAsRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => notificationsApi.markNotificationAsRead(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: NOTIFICATION_QUERY_KEYS.all });
    },
  });
}

export function useMarkAllNotificationsAsRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => notificationsApi.markAllNotificationsAsRead(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: NOTIFICATION_QUERY_KEYS.all });
    },
  });
}

export function useDeleteNotification() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => notificationsApi.deleteNotification(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: NOTIFICATION_QUERY_KEYS.all });
    },
  });
}
