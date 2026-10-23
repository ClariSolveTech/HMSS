package com.healthcare.backend.patientportal.dto;

import com.healthcare.backend.appointment.entity.AppointmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeleconsultAccessResponse {
    private Long appointmentId;
    private String appointmentNumber;
    private boolean isTeleconsultation;
    private boolean canJoin;
    private String joinUrl;
    private String reason;
    private LocalDate appointmentDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private AppointmentStatus status;
}
