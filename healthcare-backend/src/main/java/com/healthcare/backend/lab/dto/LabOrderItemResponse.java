package com.healthcare.backend.lab.dto;

import com.healthcare.backend.lab.entity.LabOrderItem;
import com.healthcare.backend.lab.entity.LabOrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabOrderItemResponse {
    private Long id;
    private Long labOrderId;
    private Long labTestId;
    private String testNameSnapshot;
    private String testCodeSnapshot;
    private LabOrderStatus status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static LabOrderItemResponse fromEntity(LabOrderItem entity) {
        if (entity == null) return null;
        return LabOrderItemResponse.builder()
                .id(entity.getId())
                .labOrderId(entity.getLabOrder() != null ? entity.getLabOrder().getId() : null)
                .labTestId(entity.getLabTest() != null ? entity.getLabTest().getId() : null)
                .testNameSnapshot(entity.getTestNameSnapshot())
                .testCodeSnapshot(entity.getTestCodeSnapshot())
                .status(entity.getStatus())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
