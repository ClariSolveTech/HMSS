package com.healthcare.backend.lab.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateLabOrderItemRequest {

    @NotNull(message = "Lab test ID is required")
    private Long labTestId;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
