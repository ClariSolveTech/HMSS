package com.healthcare.backend.prescription.dto;

import com.healthcare.backend.prescription.entity.PrescriptionItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionItemResponse {

    private Long id;
    private String medicineName;
    private String dosage;
    private String frequency;
    private String duration;
    private String route;
    private String instructions;

    public static PrescriptionItemResponse fromEntity(PrescriptionItem entity) {
        if (entity == null) return null;
        return PrescriptionItemResponse.builder()
                .id(entity.getId())
                .medicineName(entity.getMedicineName())
                .dosage(entity.getDosage())
                .frequency(entity.getFrequency())
                .duration(entity.getDuration())
                .route(entity.getRoute())
                .instructions(entity.getInstructions())
                .build();
    }
}
