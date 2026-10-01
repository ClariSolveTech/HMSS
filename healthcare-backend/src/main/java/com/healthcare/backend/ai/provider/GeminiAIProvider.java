package com.healthcare.backend.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.healthcare.backend.ai.context.ClinicalContext;
import com.healthcare.backend.ai.dto.ClinicalAssistantAction;
import com.healthcare.backend.ai.dto.ClinicalAssistantResponse;
import com.healthcare.backend.ai.prompt.ClinicalPromptBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GeminiAIProvider implements AIProvider {

    private final ClinicalPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Value("${ai.gemini.api-key:${GEMINI_API_KEY:}}")
    private String apiKey;

    @Value("${ai.gemini.model:gemini-1.5-flash}")
    private String modelName;

    public static final String CLINICAL_DISCLAIMER =
            "DISCLAIMER: This output is for clinical decision-support only and does not constitute an autonomous diagnosis or treatment decision. All clinical recommendations must be reviewed and approved by a qualified clinician.";

    @Override
    public ClinicalAssistantResponse generateClinicalResponse(ClinicalAssistantAction action, ClinicalContext context) {
        String sourceSummary = context != null ? context.buildSourceSummary() : "No context provided";

        if (apiKey == null || apiKey.trim().isEmpty()) {
            return ClinicalAssistantResponse.builder()
                    .action(action)
                    .generatedContent("[Gemini AI Provider Error] GEMINI_API_KEY environment variable is not configured on the server.")
                    .sourceSummary(sourceSummary)
                    .disclaimer(CLINICAL_DISCLAIMER)
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        String promptText = promptBuilder.buildPrompt(action, context);

        try {
            String requestUrl = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                    modelName, apiKey);

            Map<String, Object> requestBodyMap = Map.of(
                    "contents", List.of(
                            Map.of(
                                    "parts", List.of(
                                            Map.of("text", promptText)
                                    )
                            )
                    )
            );

            String requestBodyJson = objectMapper.writeValueAsString(requestBodyMap);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(requestUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return ClinicalAssistantResponse.builder()
                        .action(action)
                        .generatedContent("[Gemini AI Provider Error] Request failed with HTTP status: " + response.statusCode())
                        .sourceSummary(sourceSummary)
                        .disclaimer(CLINICAL_DISCLAIMER)
                        .timestamp(LocalDateTime.now())
                        .build();
            }

            JsonNode rootNode = objectMapper.readTree(response.body());
            JsonNode textNode = rootNode.path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text");

            String generatedText = textNode.isMissingNode() ? "No text returned from Gemini API." : textNode.asText();

            return ClinicalAssistantResponse.builder()
                    .action(action)
                    .generatedContent(generatedText)
                    .sourceSummary(sourceSummary)
                    .disclaimer(CLINICAL_DISCLAIMER)
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception ex) {
            return ClinicalAssistantResponse.builder()
                    .action(action)
                    .generatedContent("[Gemini AI Provider Error] Provider execution error occurred: " + ex.getMessage())
                    .sourceSummary(sourceSummary)
                    .disclaimer(CLINICAL_DISCLAIMER)
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    @Override
    public String getProviderName() {
        return "gemini";
    }
}
