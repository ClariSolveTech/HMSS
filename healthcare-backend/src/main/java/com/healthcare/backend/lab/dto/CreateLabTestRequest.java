package com.healthcare.backend.lab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateLabTestRequest {

    @NotBlank(message = "Test code is required")
    @Size(max = 50, message = "Test code must not exceed 50 characters")
    private String testCode;

    @NotBlank(message = "Test name is required")
    @Size(max = 150, message = "Test name must not exceed 150 characters")
    private String testName;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    @Size(max = 100, message = "Sample type must not exceed 100 characters")
    private String sampleType;

    @Size(max = 50, message = "Unit must not exceed 50 characters")
    private String unit;

    @Size(max = 200, message = "Reference range must not exceed 200 characters")
    private String referenceRange;
}
