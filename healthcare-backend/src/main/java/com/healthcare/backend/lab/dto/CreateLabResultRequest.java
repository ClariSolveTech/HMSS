package com.healthcare.backend.lab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateLabResultRequest {

    @NotNull(message = "Lab order item ID is required")
    private Long labOrderItemId;

    @NotBlank(message = "Result value is required")
    @Size(max = 255, message = "Result value must not exceed 255 characters")
    private String resultValue;

    @Size(max = 50, message = "Result unit must not exceed 50 characters")
    private String resultUnit;

    @Size(max = 200, message = "Reference range must not exceed 200 characters")
    private String referenceRange;

    private Boolean abnormalFlag = false;

    @Size(max = 1000, message = "Interpretation must not exceed 1000 characters")
    private String interpretation;

    @Size(max = 1000, message = "Remarks must not exceed 1000 characters")
    private String remarks;
}
