package com.healthcare.backend.teleconsult.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeleconsultationMeeting {
    private String meetUrl;
    private String spaceId;
    private String provider;
    private LocalDateTime createdAt;
}
