package com.healthcare.backend.teleconsult.service;

import com.healthcare.backend.teleconsult.dto.TeleconsultationMeeting;
import com.healthcare.backend.teleconsult.dto.TeleconsultationMeetingRequest;
import com.healthcare.backend.teleconsult.exception.TeleconsultationException;
import com.healthcare.backend.teleconsult.provider.GoogleMeetTeleconsultationProvider;
import com.healthcare.backend.teleconsult.provider.MockTeleconsultationProvider;
import com.healthcare.backend.teleconsult.provider.TeleconsultationProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeleconsultationService {

    private final MockTeleconsultationProvider mockProvider;
    private final GoogleMeetTeleconsultationProvider googleProvider;

    @Value("${teleconsult.provider:mock}")
    private String activeProviderName;

    public TeleconsultationProvider getActiveProvider() {
        if ("google".equalsIgnoreCase(activeProviderName)) {
            return googleProvider;
        }
        return mockProvider;
    }

    public TeleconsultationMeeting createMeeting(TeleconsultationMeetingRequest request) {
        TeleconsultationProvider provider = getActiveProvider();
        log.info("Provisioning teleconsultation meeting with active provider: {}", provider.getProviderName());
        try {
            return provider.createMeeting(request);
        } catch (TeleconsultationException te) {
            log.error("Teleconsultation provisioning error with provider {}: {}", provider.getProviderName(), te.getMessage());
            throw te;
        } catch (Exception ex) {
            log.error("Unexpected failure provisioning teleconsultation meeting with provider {}: {}", provider.getProviderName(), ex.getMessage());
            throw new TeleconsultationException("Unexpected teleconsultation error: " + ex.getMessage(), ex);
        }
    }
}
