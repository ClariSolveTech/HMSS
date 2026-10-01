package com.healthcare.backend.consultation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateConsultationRequest {

    private String chiefComplaint;
    private String symptoms;
    private String clinicalNotes;
    private String diagnosis;
    private String treatmentAdvice;
}
