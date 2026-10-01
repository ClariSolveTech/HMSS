package com.healthcare.backend.ai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalAssistantRequest {

    private Long patientId;

    private Long consultationId;

    @NotNull(message = "Action is required")
    private ClinicalAssistantAction action;
}
