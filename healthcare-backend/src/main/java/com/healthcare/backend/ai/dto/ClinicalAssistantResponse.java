package com.healthcare.backend.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalAssistantResponse {

    private ClinicalAssistantAction action;

    private String generatedContent;

    private String sourceSummary;

    private String disclaimer;

    private LocalDateTime timestamp;
}
