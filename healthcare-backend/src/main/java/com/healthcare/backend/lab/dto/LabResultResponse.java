package com.healthcare.backend.lab.dto;

import com.healthcare.backend.lab.entity.LabResult;
import com.healthcare.backend.lab.entity.LabResultStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabResultResponse {
    private Long id;
    private Long labOrderItemId;
    private Long labOrderId;
    private Long patientId;
    private String patientName;
    private Long doctorId;
    private String doctorName;
    private Long hospitalId;
    private String testNameSnapshot;
    private String testCodeSnapshot;
    private String resultValue;
    private String resultUnit;
    private String referenceRange;
    private Boolean abnormalFlag;
    private String interpretation;
    private String remarks;
    private LabResultStatus resultStatus;
    private LocalDateTime resultedAt;
    private LocalDateTime reviewedAt;
    private String reviewedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static LabResultResponse fromEntity(LabResult entity) {
        if (entity == null) return null;
        String pName = null;
        if (entity.getPatient() != null) {
            pName = entity.getPatient().getFirstName() + " " + entity.getPatient().getLastName();
        }
        String dName = null;
        if (entity.getDoctor() != null && entity.getDoctor().getUser() != null) {
            dName = "Dr. " + entity.getDoctor().getUser().getFirstName() + " " + entity.getDoctor().getUser().getLastName();
        }

        String tName = null;
        String tCode = null;
        if (entity.getLabOrderItem() != null) {
            tName = entity.getLabOrderItem().getTestNameSnapshot();
            tCode = entity.getLabOrderItem().getTestCodeSnapshot();
        }

        return LabResultResponse.builder()
                .id(entity.getId())
                .labOrderItemId(entity.getLabOrderItem() != null ? entity.getLabOrderItem().getId() : null)
                .labOrderId(entity.getLabOrder() != null ? entity.getLabOrder().getId() : null)
                .patientId(entity.getPatient() != null ? entity.getPatient().getId() : null)
                .patientName(pName)
                .doctorId(entity.getDoctor() != null ? entity.getDoctor().getId() : null)
                .doctorName(dName)
                .hospitalId(entity.getHospital() != null ? entity.getHospital().getId() : null)
                .testNameSnapshot(tName)
                .testCodeSnapshot(tCode)
                .resultValue(entity.getResultValue())
                .resultUnit(entity.getResultUnit())
                .referenceRange(entity.getReferenceRange())
                .abnormalFlag(entity.getAbnormalFlag())
                .interpretation(entity.getInterpretation())
                .remarks(entity.getRemarks())
                .resultStatus(entity.getResultStatus())
                .resultedAt(entity.getResultedAt())
                .reviewedAt(entity.getReviewedAt())
                .reviewedBy(entity.getReviewedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
