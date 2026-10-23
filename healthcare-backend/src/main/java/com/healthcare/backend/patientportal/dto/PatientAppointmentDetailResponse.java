package com.healthcare.backend.patientportal.dto;

import com.healthcare.backend.appointment.entity.AppointmentStatus;
import com.healthcare.backend.appointment.entity.AppointmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientAppointmentDetailResponse {
    private Long id;
    private String appointmentNumber;
    private String hospitalName;
    private String doctorName;
    private String specialization;
    private LocalDate appointmentDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private AppointmentStatus status;
    private AppointmentType appointmentType;
    @com.fasterxml.jackson.annotation.JsonProperty("isTeleconsultation")
    private boolean isTeleconsultation;
    @com.fasterxml.jackson.annotation.JsonProperty("canJoinConsultation")
    private boolean canJoinConsultation;
    private String joinUrl;
    private String eligibilityReason;
    private String reason;
    private String notes;
    private String cancellationReason;
    private LocalDateTime createdAt;
}
