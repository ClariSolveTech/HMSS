package com.healthcare.backend.lab.dto;

import com.healthcare.backend.lab.entity.LabOrderPriority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class UpdateLabOrderRequest {

    private LabOrderPriority priority;

    @Size(max = 1000, message = "Clinical notes must not exceed 1000 characters")
    private String clinicalNotes;

    @Valid
    private List<CreateLabOrderItemRequest> items;
}
