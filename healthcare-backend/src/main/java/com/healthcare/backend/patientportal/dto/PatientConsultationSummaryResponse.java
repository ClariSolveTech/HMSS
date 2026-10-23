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
public class PatientConsultationSummaryResponse {
    private Long id;
    private String doctorName;
    private String specialization;
    private LocalDateTime consultationDate;
    private String status;
    private String chiefComplaint;
    private String diagnosis;
    private String symptoms;
    private String treatmentAdvice;
}
