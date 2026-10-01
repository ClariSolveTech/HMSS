import { useMutation } from "@tanstack/react-query";
import { apiClient } from "@/lib/api-client";

// ─── Enum: exact values exposed by the backend ───────────────────────────────

export type ClinicalAssistantAction =
  | "SUMMARIZE_PATIENT"
  | "SUMMARIZE_CONSULTATION"
  | "ANALYZE_LAB_RESULTS"
  | "SUGGEST_CLINICAL_CONSIDERATIONS"
  | "DRAFT_CLINICAL_NOTE"
  | "EXPLAIN_PATIENT_HISTORY";

// ─── Request DTO (mirrors ClinicalAssistantRequest.java) ─────────────────────

export interface ClinicalAssistantRequest {
  /** Numeric patient ID — required for all actions */
  patientId: number;
  /** Numeric consultation ID — optional; if absent, backend uses patient context only */
  consultationId?: number | undefined;
  /** The clinical action to perform */
  action: ClinicalAssistantAction;
}

// ─── Response DTO (mirrors ClinicalAssistantResponse.java) ───────────────────

export interface ClinicalAssistantResponse {
  action: ClinicalAssistantAction;
  generatedContent: string;
  sourceSummary?: string | undefined;
  disclaimer?: string | undefined;
  /** ISO-8601 timestamp from the backend */
  timestamp?: string | undefined;
}

// ─── API service object ───────────────────────────────────────────────────────

export const aiApi = {
  requestClinicalAssistance: (data: ClinicalAssistantRequest) =>
    apiClient.post<ClinicalAssistantResponse>(
      "/api/v1/doctors/me/ai/clinical-assistant",
      data
    ),
};

// ─── React Query mutation hook ────────────────────────────────────────────────

/**
 * Submits a clinical-assistant request to the backend.
 * - Fires only on explicit doctor action (no auto-calling on mount)
 * - Does NOT invalidate patient / consultation / prescription / lab queries
 *   because the AI endpoint is read-only decision support
 */
export function useClinicalAssistant() {
  return useMutation({
    mutationFn: (data: ClinicalAssistantRequest) =>
      aiApi.requestClinicalAssistance(data),
  });
}
