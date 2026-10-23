package com.healthcare.backend.teleconsult.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.backend.teleconsult.dto.TeleconsultationMeeting;
import com.healthcare.backend.teleconsult.dto.TeleconsultationMeetingRequest;
import com.healthcare.backend.teleconsult.exception.TeleconsultationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class GoogleMeetTeleconsultationProvider implements TeleconsultationProvider {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpClient httpClient;

    @Value("${teleconsult.google.client-id:${GOOGLE_MEET_CLIENT_ID:}}")
    private String clientId;

    @Value("${teleconsult.google.client-secret:${GOOGLE_MEET_CLIENT_SECRET:}}")
    private String clientSecret;

    @Value("${teleconsult.google.refresh-token:${GOOGLE_MEET_REFRESH_TOKEN:}}")
    private String refreshToken;

    @Value("${teleconsult.google.token-url:https://oauth2.googleapis.com/token}")
    private String tokenUrl;

    @Value("${teleconsult.google.api-url:https://meet.googleapis.com/v2/spaces}")
    private String spacesApiUrl;

    // In-memory token cache
    private String cachedAccessToken;
    private Instant tokenExpiry = Instant.MIN;

    public GoogleMeetTeleconsultationProvider() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    // Constructor for testing / custom HTTP client injection
    public GoogleMeetTeleconsultationProvider(HttpClient httpClient, String clientId, String clientSecret,
                                             String refreshToken, String tokenUrl, String spacesApiUrl) {
        this.httpClient = httpClient;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.refreshToken = refreshToken;
        this.tokenUrl = tokenUrl != null ? tokenUrl : "https://oauth2.googleapis.com/token";
        this.spacesApiUrl = spacesApiUrl != null ? spacesApiUrl : "https://meet.googleapis.com/v2/spaces";
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public TeleconsultationMeeting createMeeting(TeleconsultationMeetingRequest request) {
        validateConfiguration();

        String accessToken = getOrRefreshAccessToken();
        return createGoogleMeetSpace(accessToken);
    }

    private void validateConfiguration() {
        if (clientId == null || clientId.trim().isEmpty() ||
            clientSecret == null || clientSecret.trim().isEmpty() ||
            refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new TeleconsultationException(
                    "Google Meet configuration is missing or incomplete. " +
                    "GOOGLE_MEET_CLIENT_ID, GOOGLE_MEET_CLIENT_SECRET, and GOOGLE_MEET_REFRESH_TOKEN must be configured."
            );
        }
    }

    public synchronized String getOrRefreshAccessToken() {
        // Return cached token if valid for at least another 60 seconds
        if (cachedAccessToken != null && Instant.now().isBefore(tokenExpiry.minusSeconds(60))) {
            return cachedAccessToken;
        }

        try {
            Map<String, String> formData = Map.of(
                    "client_id", clientId.trim(),
                    "client_secret", clientSecret.trim(),
                    "refresh_token", refreshToken.trim(),
                    "grant_type", "refresh_token"
            );

            String formBody = formData.entrySet().stream()
                    .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" +
                              URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));

            HttpRequest tokenRequest = HttpRequest.newBuilder()
                    .uri(URI.create(tokenUrl))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .timeout(Duration.ofSeconds(20))
                    .build();

            HttpResponse<String> tokenResponse = httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString());

            if (tokenResponse.statusCode() != 200) {
                log.error("Google OAuth token refresh failed with HTTP status {}", tokenResponse.statusCode());
                throw new TeleconsultationException(
                        "Failed to obtain Google Meet access token: HTTP " + tokenResponse.statusCode() + " - " + tokenResponse.body()
                );
            }

            JsonNode rootNode = objectMapper.readTree(tokenResponse.body());
            JsonNode tokenNode = rootNode.path("access_token");
            if (tokenNode.isMissingNode() || tokenNode.asText().isBlank()) {
                throw new TeleconsultationException("Malformed response from Google OAuth: access_token is missing");
            }

            this.cachedAccessToken = tokenNode.asText();
            int expiresIn = rootNode.path("expires_in").asInt(3600);
            this.tokenExpiry = Instant.now().plusSeconds(expiresIn);

            log.info("Successfully refreshed Google Meet access token, valid for {} seconds", expiresIn);
            return this.cachedAccessToken;

        } catch (TeleconsultationException te) {
            throw te;
        } catch (Exception ex) {
            log.error("Google Meet OAuth token refresh encountered an exception: {}", ex.getMessage());
            throw new TeleconsultationException("Google Meet authorization failure: " + ex.getMessage(), ex);
        }
    }

    private TeleconsultationMeeting createGoogleMeetSpace(String accessToken) {
        try {
            // Space creation body: {} or config with open access
            String requestBodyJson = "{}";

            HttpRequest spaceRequest = HttpRequest.newBuilder()
                    .uri(URI.create(spacesApiUrl))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                    .timeout(Duration.ofSeconds(20))
                    .build();

            HttpResponse<String> spaceResponse = httpClient.send(spaceRequest, HttpResponse.BodyHandlers.ofString());

            if (spaceResponse.statusCode() < 200 || spaceResponse.statusCode() >= 300) {
                log.error("Google Meet space creation failed with HTTP status {}", spaceResponse.statusCode());
                throw new TeleconsultationException(
                        "Google Meet API space creation failed: HTTP " + spaceResponse.statusCode() + " - " + spaceResponse.body()
                );
            }

            JsonNode rootNode = objectMapper.readTree(spaceResponse.body());
            JsonNode nameNode = rootNode.path("name");
            JsonNode meetingUriNode = rootNode.path("meetingUri");

            if (nameNode.isMissingNode() || meetingUriNode.isMissingNode() ||
                nameNode.asText().isBlank() || meetingUriNode.asText().isBlank()) {
                throw new TeleconsultationException("Malformed response from Google Meet API: missing 'name' or 'meetingUri'");
            }

            String spaceId = nameNode.asText();
            String meetUrl = meetingUriNode.asText();

            log.info("Successfully created Google Meet space '{}' with URI '{}'", spaceId, meetUrl);

            return TeleconsultationMeeting.builder()
                    .meetUrl(meetUrl)
                    .spaceId(spaceId)
                    .provider("google")
                    .createdAt(LocalDateTime.now())
                    .build();

        } catch (TeleconsultationException te) {
            throw te;
        } catch (Exception ex) {
            log.error("Google Meet API space creation encountered an exception: {}", ex.getMessage());
            throw new TeleconsultationException("Google Meet API execution failure: " + ex.getMessage(), ex);
        }
    }

    @Override
    public String getProviderName() {
        return "google";
    }
}
