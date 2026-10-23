package com.healthcare.backend.patientportal.dto;

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
public class PatientPrescriptionSummaryResponse {
    private Long id;
    private String prescriptionCode;
    private String doctorName;
    private String specialization;
    private LocalDateTime issuedDate;
    private String status;
    private int itemCount;
    private String notes;
}
