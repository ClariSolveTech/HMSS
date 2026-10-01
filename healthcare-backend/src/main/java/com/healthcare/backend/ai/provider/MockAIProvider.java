package com.healthcare.backend.ai.provider;

import com.healthcare.backend.ai.context.ClinicalContext;
import com.healthcare.backend.ai.dto.ClinicalAssistantAction;
import com.healthcare.backend.ai.dto.ClinicalAssistantResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MockAIProvider implements AIProvider {

    public static final String CLINICAL_DISCLAIMER =
            "DISCLAIMER: This output is for clinical decision-support only and does not constitute an autonomous diagnosis or treatment decision. All clinical recommendations must be reviewed and approved by a qualified clinician.";

    @Override
    public ClinicalAssistantResponse generateClinicalResponse(ClinicalAssistantAction action, ClinicalContext context) {
        String sourceSummary = context != null ? context.buildSourceSummary() : "No context provided";

        String generatedContent = String.format(
                "[Mock AI Provider - Development Mode]\n" +
                "Action requested: %s\n" +
                "Context summary: %s\n" +
                "Development AI provider response. No clinical inference was performed.",
                action,
                sourceSummary
        );

        return ClinicalAssistantResponse.builder()
                .action(action)
                .generatedContent(generatedContent)
                .sourceSummary(sourceSummary)
                .disclaimer(CLINICAL_DISCLAIMER)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public String getProviderName() {
        return "mock";
    }
}
