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
public class CohereAIProvider implements AIProvider {

    private final ClinicalPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Value("${ai.cohere.api-key:${COHERE_API_KEY:}}")
    private String apiKey;

    @Value("${ai.cohere.model:command-r-plus-08-2024}")
    private String modelName;

    public static final String CLINICAL_DISCLAIMER =
            "DISCLAIMER: This output is for clinical decision-support only and does not constitute an autonomous diagnosis or treatment decision. All clinical recommendations must be reviewed and approved by a qualified clinician.";

    @Override
    public ClinicalAssistantResponse generateClinicalResponse(ClinicalAssistantAction action, ClinicalContext context) {
        String sourceSummary = context != null ? context.buildSourceSummary() : "No context provided";

        if (apiKey == null || apiKey.trim().isEmpty()) {
            return ClinicalAssistantResponse.builder()
                    .action(action)
                    .generatedContent("[Cohere AI Provider Error] COHERE_API_KEY environment variable is not configured on the server.")
                    .sourceSummary(sourceSummary)
                    .disclaimer(CLINICAL_DISCLAIMER)
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        String promptText = promptBuilder.buildPrompt(action, context);

        try {
            String requestUrl = "https://api.cohere.com/v2/chat";

            Map<String, Object> requestBodyMap = Map.of(
                    "model", modelName,
                    "messages", List.of(
                            Map.of(
                                    "role", "user",
                                    "content", promptText
                            )
                    )
            );

            String requestBodyJson = objectMapper.writeValueAsString(requestBodyMap);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(requestUrl))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + apiKey.trim())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return ClinicalAssistantResponse.builder()
                        .action(action)
                        .generatedContent("[Cohere AI Provider Error] Request failed with HTTP status: " + response.statusCode())
                        .sourceSummary(sourceSummary)
                        .disclaimer(CLINICAL_DISCLAIMER)
                        .timestamp(LocalDateTime.now())
                        .build();
            }

            JsonNode rootNode = objectMapper.readTree(response.body());
            String generatedText = parseV2ResponseText(rootNode);

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
                    .generatedContent("[Cohere AI Provider Error] Provider execution error occurred: " + ex.getMessage())
                    .sourceSummary(sourceSummary)
                    .disclaimer(CLINICAL_DISCLAIMER)
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    private String parseV2ResponseText(JsonNode rootNode) {
        if (rootNode == null) {
            return "No response received from Cohere API.";
        }

        // Cohere v2 Chat API response: message.content[0].text
        JsonNode contentArray = rootNode.path("message").path("content");
        if (contentArray.isArray() && contentArray.size() > 0) {
            JsonNode textNode = contentArray.get(0).path("text");
            if (!textNode.isMissingNode() && !textNode.isNull() && !textNode.asText().isBlank()) {
                return textNode.asText();
            }
        }

        // Fallback for v1 or direct text field
        JsonNode directTextNode = rootNode.path("text");
        if (!directTextNode.isMissingNode() && !directTextNode.isNull() && !directTextNode.asText().isBlank()) {
            return directTextNode.asText();
        }

        return "No text returned from Cohere API.";
    }

    @Override
    public String getProviderName() {
        return "cohere";
    }
}
