package com.healthcare.backend.lab.dto;

import com.healthcare.backend.lab.entity.LabTest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabTestResponse {
    private Long id;
    private Long hospitalId;
    private String testCode;
    private String testName;
    private String description;
    private String category;
    private String sampleType;
    private String unit;
    private String referenceRange;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static LabTestResponse fromEntity(LabTest entity) {
        if (entity == null) return null;
        return LabTestResponse.builder()
                .id(entity.getId())
                .hospitalId(entity.getHospital() != null ? entity.getHospital().getId() : null)
                .testCode(entity.getTestCode())
                .testName(entity.getTestName())
                .description(entity.getDescription())
                .category(entity.getCategory())
                .sampleType(entity.getSampleType())
                .unit(entity.getUnit())
                .referenceRange(entity.getReferenceRange())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
