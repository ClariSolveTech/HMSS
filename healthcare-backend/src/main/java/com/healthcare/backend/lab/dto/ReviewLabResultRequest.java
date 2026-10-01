package com.healthcare.backend.lab.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReviewLabResultRequest {

    @Size(max = 1000, message = "Interpretation must not exceed 1000 characters")
    private String interpretation;

    @Size(max = 1000, message = "Remarks must not exceed 1000 characters")
    private String remarks;
}
