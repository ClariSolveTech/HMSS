package com.healthcare.backend.patientportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientPrescriptionDetailResponse {
    private Long id;
    private String prescriptionCode;
    private String doctorName;
    private String specialization;
    private String diagnosis;
    private LocalDateTime issuedDate;
    private String status;
    private String notes;
    private List<PatientPrescriptionItemResponse> items;
}
