package com.healthcare.backend.consultation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateConsultationRequest {

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    private Long doctorId;
    private Long appointmentId;
    private Long queueEntryId;

    private String chiefComplaint;
    private String symptoms;
    private String clinicalNotes;
    private String diagnosis;
    private String treatmentAdvice;
}
