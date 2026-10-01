package com.healthcare.backend.prescription.dto;

import com.healthcare.backend.prescription.entity.Prescription;
import com.healthcare.backend.prescription.entity.PrescriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionResponse {

    private Long id;
    private String prescriptionCode;
    private Long hospitalId;
    private Long doctorId;
    private Long patientId;
    private Long consultationId;
    private PrescriptionStatus status;
    private String notes;
    private LocalDateTime issuedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<PrescriptionItemResponse> items;

    public static PrescriptionResponse fromEntity(Prescription entity) {
        if (entity == null) return null;
        return PrescriptionResponse.builder()
                .id(entity.getId())
                .prescriptionCode(entity.getPrescriptionCode())
                .hospitalId(entity.getHospital() != null ? entity.getHospital().getId() : null)
                .doctorId(entity.getDoctor() != null ? entity.getDoctor().getId() : null)
                .patientId(entity.getPatient() != null ? entity.getPatient().getId() : null)
                .consultationId(entity.getConsultation() != null ? entity.getConsultation().getId() : null)
                .status(entity.getStatus())
                .notes(entity.getNotes())
                .issuedAt(entity.getIssuedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .items(entity.getItems() != null ? entity.getItems().stream()
                        .map(PrescriptionItemResponse::fromEntity)
                        .collect(Collectors.toList()) : Collections.emptyList())
                .build();
    }
}
