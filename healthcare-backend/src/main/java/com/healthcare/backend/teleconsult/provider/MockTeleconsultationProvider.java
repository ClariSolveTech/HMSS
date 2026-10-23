package com.healthcare.backend.teleconsult.provider;

import com.healthcare.backend.teleconsult.dto.TeleconsultationMeeting;
import com.healthcare.backend.teleconsult.dto.TeleconsultationMeetingRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Slf4j
public class MockTeleconsultationProvider implements TeleconsultationProvider {

    public static final String MOCK_URL_PREFIX = "https://meet.mock.internal/teleconsult/simulated-";
    public static final String MOCK_SPACE_PREFIX = "spaces/mock-simulated-";

    @Override
    public TeleconsultationMeeting createMeeting(TeleconsultationMeetingRequest request) {
        String refId;
        if (request != null && request.getDoctorId() != null && request.getAppointmentDate() != null && request.getStartTime() != null) {
            refId = String.format("%d-%s-%s", request.getDoctorId(), request.getAppointmentDate(), request.getStartTime().toString().replace(":", ""));
        } else {
            refId = UUID.randomUUID().toString().substring(0, 8);
        }

        String spaceId = MOCK_SPACE_PREFIX + refId;
        String meetUrl = MOCK_URL_PREFIX + refId;

        log.info("[MockTeleconsultationProvider] Generated simulated meeting space '{}' with URL '{}'", spaceId, meetUrl);

        return TeleconsultationMeeting.builder()
                .meetUrl(meetUrl)
                .spaceId(spaceId)
                .provider("mock")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Override
    public String getProviderName() {
        return "mock";
    }
}
