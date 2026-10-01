package com.healthcare.backend.patient.dto;

import com.healthcare.backend.patient.entity.BloodGroup;
import com.healthcare.backend.patient.entity.Gender;
import com.healthcare.backend.patient.entity.PatientStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientResponse {

    private Long id;
    private String patientCode;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String mobileNumber;
    private String email;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String emergencyContactName;
    private String emergencyContactNumber;
    private BloodGroup bloodGroup;
    private PatientStatus status;
    private Long hospitalId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
