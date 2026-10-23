package com.healthcare.backend.teleconsult.provider;

import com.healthcare.backend.teleconsult.dto.TeleconsultationMeeting;
import com.healthcare.backend.teleconsult.dto.TeleconsultationMeetingRequest;

public interface TeleconsultationProvider {

    TeleconsultationMeeting createMeeting(TeleconsultationMeetingRequest request);

    String getProviderName();
}
