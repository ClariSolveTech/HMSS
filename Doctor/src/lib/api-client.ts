import { getAccessToken, logout } from "./auth";

const BASE_URL = import.meta.env["VITE_API_BASE_URL"] || "http://localhost:8081";

export class ApiError extends Error {
  status: number;
  data: any;

  constructor(status: number, message: string, data?: any) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.data = data;
  }
}

async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const token = getAccessToken();
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(options.headers as Record<string, string>),
  };

  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  const url = endpoint.startsWith("http") ? endpoint : `${BASE_URL}${endpoint}`;

  const config: RequestInit = {
    ...options,
    headers,
  };

  try {
    const response = await fetch(url, config);

    if (response.status === 401) {
      logout();
      throw new ApiError(401, "Your session has expired. Please log in again.");
    }

    if (!response.ok) {
      let errorData: any;
      try {
        errorData = await response.json();
      } catch {
        errorData = null;
      }

      const message =
        errorData?.message ||
        errorData?.error ||
        getErrorMessageByStatus(response.status);

      throw new ApiError(response.status, message, errorData);
    }

    if (response.status === 204) {
      return {} as T;
    }

    return await response.json();
  } catch (err) {
    if (err instanceof ApiError) {
      throw err;
    }
    throw new ApiError(0, "Network error or server unreachable. Please check connection.");
  }
}

function getErrorMessageByStatus(status: number): string {
  switch (status) {
    case 400:
      return "Invalid request parameters.";
    case 403:
      return "You are not authorized to perform this action.";
    case 404:
      return "Resource not found.";
    case 409:
      return "Queue entry state changed or duplicate entry.";
    case 500:
      return "Internal server error. Please try again.";
    default:
      return `Request failed with status ${status}`;
  }
}

export const apiClient = {
  get: <T>(url: string, options?: RequestInit) => request<T>(url, { ...options, method: "GET" }),
  post: <T>(url: string, body?: any, options?: RequestInit) =>
    request<T>(url, { ...options, method: "POST", body: body !== undefined ? JSON.stringify(body) : null }),
  put: <T>(url: string, body?: any, options?: RequestInit) =>
    request<T>(url, { ...options, method: "PUT", body: body !== undefined ? JSON.stringify(body) : null }),
  patch: <T>(url: string, body?: any, options?: RequestInit) =>
    request<T>(url, { ...options, method: "PATCH", body: body !== undefined ? JSON.stringify(body) : null }),
  delete: <T>(url: string, options?: RequestInit) => request<T>(url, { ...options, method: "DELETE" }),
};
