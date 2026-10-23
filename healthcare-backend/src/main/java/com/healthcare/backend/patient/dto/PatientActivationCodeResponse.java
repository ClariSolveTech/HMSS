package com.healthcare.backend.patient.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientActivationCodeResponse {
    private Long patientId;
    private String patientCode;
    private String patientName;
    private String mobileNumber;
    private String activationCode;
    private LocalDateTime expiresAt;
    private String instructions;
}
