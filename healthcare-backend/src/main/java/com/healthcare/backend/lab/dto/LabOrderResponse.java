package com.healthcare.backend.lab.dto;

import com.healthcare.backend.lab.entity.LabOrder;
import com.healthcare.backend.lab.entity.LabOrderPriority;
import com.healthcare.backend.lab.entity.LabOrderStatus;
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
public class LabOrderResponse {
    private Long id;
    private String orderNumber;
    private Long hospitalId;
    private Long patientId;
    private String patientName;
    private Long doctorId;
    private String doctorName;
    private Long consultationId;
    private LabOrderStatus status;
    private LabOrderPriority priority;
    private String clinicalNotes;
    private LocalDateTime orderedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<LabOrderItemResponse> items;

    public static LabOrderResponse fromEntity(LabOrder entity) {
        if (entity == null) return null;
        String pName = null;
        if (entity.getPatient() != null) {
            pName = entity.getPatient().getFirstName() + " " + entity.getPatient().getLastName();
        }
        String dName = null;
        if (entity.getDoctor() != null && entity.getDoctor().getUser() != null) {
            dName = "Dr. " + entity.getDoctor().getUser().getFirstName() + " " + entity.getDoctor().getUser().getLastName();
        }

        return LabOrderResponse.builder()
                .id(entity.getId())
                .orderNumber(entity.getOrderNumber())
                .hospitalId(entity.getHospital() != null ? entity.getHospital().getId() : null)
                .patientId(entity.getPatient() != null ? entity.getPatient().getId() : null)
                .patientName(pName)
                .doctorId(entity.getDoctor() != null ? entity.getDoctor().getId() : null)
                .doctorName(dName)
                .consultationId(entity.getConsultation() != null ? entity.getConsultation().getId() : null)
                .status(entity.getStatus())
                .priority(entity.getPriority())
                .clinicalNotes(entity.getClinicalNotes())
                .orderedAt(entity.getOrderedAt())
                .completedAt(entity.getCompletedAt())
                .cancelledAt(entity.getCancelledAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .items(entity.getItems() != null ? entity.getItems().stream()
                        .map(LabOrderItemResponse::fromEntity)
                        .collect(Collectors.toList()) : Collections.emptyList())
                .build();
    }
}
