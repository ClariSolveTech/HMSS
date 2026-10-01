package com.healthcare.backend.prescription.dto;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePrescriptionRequest {

    private String notes;

    @Valid
    private List<PrescriptionItemRequest> items;
}
