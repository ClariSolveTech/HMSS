package com.healthcare.backend.lab.dto;

import com.healthcare.backend.lab.entity.LabOrderPriority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CreateLabOrderRequest {

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    private Long consultationId;

    private LabOrderPriority priority = LabOrderPriority.NORMAL;

    @Size(max = 1000, message = "Clinical notes must not exceed 1000 characters")
    private String clinicalNotes;

    @NotEmpty(message = "At least one lab test item is required")
    @Valid
    private List<CreateLabOrderItemRequest> items = new ArrayList<>();
}
