package com.healthcare.backend.teleconsult;

import com.healthcare.backend.teleconsult.dto.TeleconsultationMeeting;
import com.healthcare.backend.teleconsult.dto.TeleconsultationMeetingRequest;
import com.healthcare.backend.teleconsult.exception.TeleconsultationException;
import com.healthcare.backend.teleconsult.provider.GoogleMeetTeleconsultationProvider;
import com.healthcare.backend.teleconsult.provider.MockTeleconsultationProvider;
import com.healthcare.backend.teleconsult.service.TeleconsultationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeleconsultationProviderTest {

    private MockTeleconsultationProvider mockProvider;

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    private HttpResponse<String> mockHttpResponse;

    @BeforeEach
    void setUp() {
        mockProvider = new MockTeleconsultationProvider();
    }

    @Test
    @DisplayName("Mock Provider: creates simulated meeting references without external network calls")
    void testMockProvider_createMeeting_returnsSimulatedReference() {
        TeleconsultationMeetingRequest req = TeleconsultationMeetingRequest.builder()
                .hospitalId(1L)
                .doctorId(10L)
                .doctorName("Dr. Rao")
                .patientId(20L)
                .patientName("John Doe")
                .appointmentDate(LocalDate.now().plusDays(2))
                .startTime(LocalTime.of(11, 0))
                .durationMinutes(15)
                .topic("Follow up")
                .build();

        TeleconsultationMeeting meeting = mockProvider.createMeeting(req);

        assertNotNull(meeting);
        assertEquals("mock", meeting.getProvider());
        assertNotNull(meeting.getCreatedAt());
        assertTrue(meeting.getMeetUrl().startsWith("https://meet.mock.internal/teleconsult/simulated-"));
        assertTrue(meeting.getSpaceId().startsWith("spaces/mock-simulated-"));
        assertEquals("mock", mockProvider.getProviderName());
    }

    @Test
    @DisplayName("Google Provider: fails fast when credentials are not configured")
    void testGoogleProvider_missingConfiguration_throwsException() {
        GoogleMeetTeleconsultationProvider googleProvider = new GoogleMeetTeleconsultationProvider();

        TeleconsultationMeetingRequest req = TeleconsultationMeetingRequest.builder()
                .hospitalId(1L)
                .doctorId(10L)
                .appointmentDate(LocalDate.now().plusDays(2))
                .startTime(LocalTime.of(11, 0))
                .build();

        TeleconsultationException ex = assertThrows(TeleconsultationException.class,
                () -> googleProvider.createMeeting(req));

        assertTrue(ex.getMessage().contains("GOOGLE_MEET_CLIENT_ID"));
        assertEquals("google", googleProvider.getProviderName());
    }

    @Test
    @DisplayName("Google Provider: successfully exchanges token and provisions space with mock HTTP client")
    void testGoogleProvider_successfulOAuthAndSpaceCreation() throws Exception {
        GoogleMeetTeleconsultationProvider googleProvider = new GoogleMeetTeleconsultationProvider(
                mockHttpClient,
                "mock-client-id",
                "mock-client-secret",
                "mock-refresh-token",
                "https://oauth2.googleapis.com/token",
                "https://meet.googleapis.com/v2/spaces"
        );

        HttpResponse<String> tokenResponse = mock(HttpResponse.class);
        when(tokenResponse.statusCode()).thenReturn(200);
        when(tokenResponse.body()).thenReturn("{\"access_token\": \"mock_access_token_123\", \"expires_in\": 3600}");

        HttpResponse<String> spaceResponse = mock(HttpResponse.class);
        when(spaceResponse.statusCode()).thenReturn(200);
        when(spaceResponse.body()).thenReturn("""
            {
                "name": "spaces/xyz-abcd-efg",
                "meetingUri": "https://meet.google.com/xyz-abcd-efg",
                "meetingCode": "xyz-abcd-efg"
            }
            """);

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(tokenResponse)
                .thenReturn(spaceResponse);

        TeleconsultationMeetingRequest req = TeleconsultationMeetingRequest.builder()
                .hospitalId(1L)
                .doctorId(10L)
                .doctorName("Dr. Rao")
                .appointmentDate(LocalDate.now().plusDays(2))
                .startTime(LocalTime.of(11, 0))
                .build();

        TeleconsultationMeeting meeting = googleProvider.createMeeting(req);

        assertNotNull(meeting);
        assertEquals("google", meeting.getProvider());
        assertEquals("https://meet.google.com/xyz-abcd-efg", meeting.getMeetUrl());
        assertEquals("spaces/xyz-abcd-efg", meeting.getSpaceId());

        // Verify token caching: second call within expiry should NOT call token endpoint again
        HttpResponse<String> secondSpaceResponse = mock(HttpResponse.class);
        when(secondSpaceResponse.statusCode()).thenReturn(200);
        when(secondSpaceResponse.body()).thenReturn("""
            {
                "name": "spaces/second-space",
                "meetingUri": "https://meet.google.com/second-space"
            }
            """);

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(secondSpaceResponse);

        TeleconsultationMeeting secondMeeting = googleProvider.createMeeting(req);
        assertEquals("https://meet.google.com/second-space", secondMeeting.getMeetUrl());
    }

    @Test
    @DisplayName("Google Provider: throws TeleconsultationException on OAuth HTTP 401 error")
    void testGoogleProvider_oauthFailure_throwsException() throws Exception {
        GoogleMeetTeleconsultationProvider googleProvider = new GoogleMeetTeleconsultationProvider(
                mockHttpClient,
                "invalid-client-id",
                "invalid-secret",
                "invalid-refresh",
                "https://oauth2.googleapis.com/token",
                "https://meet.googleapis.com/v2/spaces"
        );

        HttpResponse<String> tokenResponse = mock(HttpResponse.class);
        when(tokenResponse.statusCode()).thenReturn(401);
        when(tokenResponse.body()).thenReturn("{\"error\": \"invalid_client\"}");

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(tokenResponse);

        TeleconsultationMeetingRequest req = TeleconsultationMeetingRequest.builder().build();

        TeleconsultationException ex = assertThrows(TeleconsultationException.class,
                () -> googleProvider.createMeeting(req));

        assertTrue(ex.getMessage().contains("Failed to obtain Google Meet access token"));
    }

    @Test
    @DisplayName("Google Provider: throws TeleconsultationException on Space Creation HTTP 403 error")
    void testGoogleProvider_spaceCreationFailure_throwsException() throws Exception {
        GoogleMeetTeleconsultationProvider googleProvider = new GoogleMeetTeleconsultationProvider(
                mockHttpClient,
                "client-id",
                "secret",
                "refresh",
                "https://oauth2.googleapis.com/token",
                "https://meet.googleapis.com/v2/spaces"
        );

        HttpResponse<String> tokenResponse = mock(HttpResponse.class);
        when(tokenResponse.statusCode()).thenReturn(200);
        when(tokenResponse.body()).thenReturn("{\"access_token\": \"valid_token\", \"expires_in\": 3600}");

        HttpResponse<String> spaceResponse = mock(HttpResponse.class);
        when(spaceResponse.statusCode()).thenReturn(403);
        when(spaceResponse.body()).thenReturn("{\"error\": {\"message\": \"Google Meet API not enabled\"}}");

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(tokenResponse)
                .thenReturn(spaceResponse);

        TeleconsultationMeetingRequest req = TeleconsultationMeetingRequest.builder().build();

        TeleconsultationException ex = assertThrows(TeleconsultationException.class,
                () -> googleProvider.createMeeting(req));

        assertTrue(ex.getMessage().contains("Google Meet API space creation failed"));
    }

    @Test
    @DisplayName("Google Provider: throws TeleconsultationException on malformed space response")
    void testGoogleProvider_malformedResponse_throwsException() throws Exception {
        GoogleMeetTeleconsultationProvider googleProvider = new GoogleMeetTeleconsultationProvider(
                mockHttpClient,
                "client-id",
                "secret",
                "refresh",
                "https://oauth2.googleapis.com/token",
                "https://meet.googleapis.com/v2/spaces"
        );

        HttpResponse<String> tokenResponse = mock(HttpResponse.class);
        when(tokenResponse.statusCode()).thenReturn(200);
        when(tokenResponse.body()).thenReturn("{\"access_token\": \"valid_token\", \"expires_in\": 3600}");

        HttpResponse<String> spaceResponse = mock(HttpResponse.class);
        when(spaceResponse.statusCode()).thenReturn(200);
        when(spaceResponse.body()).thenReturn("{\"invalid\": \"payload\"}"); // missing name and meetingUri

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(tokenResponse)
                .thenReturn(spaceResponse);

        TeleconsultationMeetingRequest req = TeleconsultationMeetingRequest.builder().build();

        TeleconsultationException ex = assertThrows(TeleconsultationException.class,
                () -> googleProvider.createMeeting(req));

        assertTrue(ex.getMessage().contains("Malformed response from Google Meet API"));
    }

    @Test
    @DisplayName("TeleconsultationService: selects provider based on activeProviderName configuration")
    void testTeleconsultationService_providerSelection() {
        GoogleMeetTeleconsultationProvider googleProvider = mock(GoogleMeetTeleconsultationProvider.class);
        when(googleProvider.getProviderName()).thenReturn("google");

        TeleconsultationService service = new TeleconsultationService(mockProvider, googleProvider);

        // Default / mock
        ReflectionTestUtils.setField(service, "activeProviderName", "mock");
        assertEquals("mock", service.getActiveProvider().getProviderName());

        // Google selection
        ReflectionTestUtils.setField(service, "activeProviderName", "google");
        assertEquals("google", service.getActiveProvider().getProviderName());

        // Unknown fallback to mock
        ReflectionTestUtils.setField(service, "activeProviderName", "other");
        assertEquals("mock", service.getActiveProvider().getProviderName());
    }
}
