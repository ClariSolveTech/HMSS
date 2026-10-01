package com.healthcare.backend.prescription.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePrescriptionRequest {

    @NotNull(message = "Consultation ID is required")
    private Long consultationId;

    private String notes;

    @NotEmpty(message = "Prescription must contain at least one item")
    @Valid
    private List<PrescriptionItemRequest> items;
}
