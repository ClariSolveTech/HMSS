package com.healthcare.backend.doctor.dto;

import com.healthcare.backend.doctor.entity.Doctor;
import com.healthcare.backend.doctor.entity.DoctorStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class DoctorResponse {

    private Long id;
    private String doctorCode;
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String mobileNumber;
    private String specialization;
    private String qualification;
    private String licenseNumber;
    private BigDecimal consultationFee;
    private DoctorStatus status;
    private Long hospitalId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DoctorResponse fromEntity(Doctor doctor) {
        return DoctorResponse.builder()
                .id(doctor.getId())
                .doctorCode(doctor.getDoctorCode())
                .userId(doctor.getUser() != null ? doctor.getUser().getId() : null)
                .firstName(doctor.getUser() != null ? doctor.getUser().getFirstName() : null)
                .lastName(doctor.getUser() != null ? doctor.getUser().getLastName() : null)
                .email(doctor.getUser() != null ? doctor.getUser().getEmail() : null)
                .mobileNumber(doctor.getUser() != null ? doctor.getUser().getMobileNumber() : null)
                .specialization(doctor.getSpecialization())
                .qualification(doctor.getQualification())
                .licenseNumber(doctor.getLicenseNumber())
                .consultationFee(doctor.getConsultationFee())
                .status(doctor.getStatus())
                .hospitalId(doctor.getHospital() != null ? doctor.getHospital().getId() : null)
                .createdAt(doctor.getCreatedAt())
                .updatedAt(doctor.getUpdatedAt())
                .build();
    }
}
