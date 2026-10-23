package com.healthcare.backend.teleconsult.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeleconsultationMeetingRequest {
    private Long appointmentId;
    private Long hospitalId;
    private Long doctorId;
    private String doctorName;
    private Long patientId;
    private String patientName;
    private LocalDate appointmentDate;
    private LocalTime startTime;
    private Integer durationMinutes;
    private String topic;
}
