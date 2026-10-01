package com.healthcare.backend.lab.dto;

import com.healthcare.backend.lab.entity.LabOrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateLabOrderStatusRequest {

    @NotNull(message = "Status is required")
    private LabOrderStatus status;
}
