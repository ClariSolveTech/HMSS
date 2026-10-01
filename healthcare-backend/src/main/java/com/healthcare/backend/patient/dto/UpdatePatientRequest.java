package com.healthcare.backend.patient.dto;

import com.healthcare.backend.patient.entity.BloodGroup;
import com.healthcare.backend.patient.entity.Gender;
import com.healthcare.backend.patient.entity.PatientStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePatientRequest {

    private String firstName;
    private String lastName;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    private Gender gender;

    @Size(min = 10, max = 15, message = "Mobile number must be between 10 and 15 digits")
    private String mobileNumber;

    @Email(message = "Invalid email format")
    private String email;

    private String address;
    private String city;
    private String state;
    private String pincode;

    private String emergencyContactName;
    private String emergencyContactNumber;

    private BloodGroup bloodGroup;
    private PatientStatus status;
}
