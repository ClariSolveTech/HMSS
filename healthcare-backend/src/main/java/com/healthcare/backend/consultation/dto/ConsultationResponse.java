package com.healthcare.backend.consultation.dto;

import com.healthcare.backend.consultation.entity.Consultation;
import com.healthcare.backend.consultation.entity.ConsultationStatus;
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
public class ConsultationResponse {

    private Long id;
    private Long hospitalId;
    private String hospitalName;
    private Long doctorId;
    private String doctorName;
    private String doctorSpecialization;
    private Long patientId;
    private String patientName;
    private String patientCode;
    private Long appointmentId;
    private Long queueEntryId;
    private Integer tokenNumber;
    private LocalDateTime consultationDate;
    private String chiefComplaint;
    private String symptoms;
    private String clinicalNotes;
    private String diagnosis;
    private String treatmentAdvice;
    private ConsultationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ConsultationResponse fromEntity(Consultation entity) {
        if (entity == null) return null;

        String doctorFullName = null;
        String spec = null;
        if (entity.getDoctor() != null) {
            spec = entity.getDoctor().getSpecialization();
            if (entity.getDoctor().getUser() != null) {
                doctorFullName = entity.getDoctor().getUser().getFirstName() + " " + entity.getDoctor().getUser().getLastName();
            }
        }

        String patientFullName = null;
        String pCode = null;
        if (entity.getPatient() != null) {
            patientFullName = entity.getPatient().getFirstName() + " " + entity.getPatient().getLastName();
            pCode = entity.getPatient().getPatientCode();
        }

        Integer token = entity.getQueueEntry() != null ? entity.getQueueEntry().getTokenNumber() : null;

        return ConsultationResponse.builder()
                .id(entity.getId())
                .hospitalId(entity.getHospital() != null ? entity.getHospital().getId() : null)
                .hospitalName(entity.getHospital() != null ? entity.getHospital().getName() : null)
                .doctorId(entity.getDoctor() != null ? entity.getDoctor().getId() : null)
                .doctorName(doctorFullName)
                .doctorSpecialization(spec)
                .patientId(entity.getPatient() != null ? entity.getPatient().getId() : null)
                .patientName(patientFullName)
                .patientCode(pCode)
                .appointmentId(entity.getAppointment() != null ? entity.getAppointment().getId() : null)
                .queueEntryId(entity.getQueueEntry() != null ? entity.getQueueEntry().getId() : null)
                .tokenNumber(token)
                .consultationDate(entity.getConsultationDate())
                .chiefComplaint(entity.getChiefComplaint())
                .symptoms(entity.getSymptoms())
                .clinicalNotes(entity.getClinicalNotes())
                .diagnosis(entity.getDiagnosis())
                .treatmentAdvice(entity.getTreatmentAdvice())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
