package com.healthcare.backend.doctor.dto;

import com.healthcare.backend.doctor.entity.DoctorStatus;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateDoctorRequest {

    private String specialization;

    private String qualification;

    private String licenseNumber;

    @DecimalMin(value = "0.0", inclusive = true, message = "Consultation fee cannot be negative")
    private BigDecimal consultationFee;

    private DoctorStatus status;
}
