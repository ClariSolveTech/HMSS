package com.healthcare.backend.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.backend.ai.context.ClinicalContext;
import com.healthcare.backend.ai.dto.ClinicalAssistantAction;
import com.healthcare.backend.ai.dto.ClinicalAssistantResponse;
import com.healthcare.backend.ai.prompt.ClinicalPromptBuilder;
import com.healthcare.backend.ai.provider.CohereAIProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CohereAIProviderTest {

    private CohereAIProvider provider;
    private ClinicalPromptBuilder promptBuilder;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        promptBuilder = new ClinicalPromptBuilder();
        provider = new CohereAIProvider(promptBuilder);
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("1. Provider name is cohere")
    void testProviderName() {
        assertEquals("cohere", provider.getProviderName());
    }

    @Test
    @DisplayName("2. Missing API key returns safe descriptive error message without throwing exception")
    void testMissingApiKeyReturnsSafeError() {
        ClinicalAssistantResponse response = provider.generateClinicalResponse(
                ClinicalAssistantAction.SUMMARIZE_PATIENT,
                null
        );

        assertNotNull(response);
        assertEquals(ClinicalAssistantAction.SUMMARIZE_PATIENT, response.getAction());
        assertTrue(response.getGeneratedContent().contains("COHERE_API_KEY environment variable is not configured"));
        assertNotNull(response.getDisclaimer());
        assertTrue(response.getDisclaimer().contains("clinical decision-support"));
    }

    @Test
    @DisplayName("3. Disclaimer is attached to all responses")
    void testDisclaimerPresent() {
        ClinicalAssistantResponse response = provider.generateClinicalResponse(
                ClinicalAssistantAction.ANALYZE_LAB_RESULTS,
                null
        );

        assertNotNull(response.getDisclaimer());
        assertEquals(CohereAIProvider.CLINICAL_DISCLAIMER, response.getDisclaimer());
    }

    @Test
    @DisplayName("4. Response contains no sensitive tokens or secrets")
    void testNoSecretsExposed() {
        ClinicalAssistantResponse response = provider.generateClinicalResponse(
                ClinicalAssistantAction.DRAFT_CLINICAL_NOTE,
                null
        );

        assertFalse(response.getGeneratedContent().contains("secret"));
        assertFalse(response.getGeneratedContent().contains("Bearer"));
    }
}
