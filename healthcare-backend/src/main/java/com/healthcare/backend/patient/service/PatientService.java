package com.healthcare.backend.patient.service;

import com.healthcare.backend.auth.entity.RoleName;
import com.healthcare.backend.auth.security.UserPrincipal;
import com.healthcare.backend.hospital.entity.Hospital;
import com.healthcare.backend.hospital.repository.HospitalRepository;
import com.healthcare.backend.patient.dto.CreatePatientRequest;
import com.healthcare.backend.patient.dto.PatientResponse;
import com.healthcare.backend.patient.dto.UpdatePatientRequest;
import com.healthcare.backend.patient.entity.Patient;
import com.healthcare.backend.patient.entity.PatientStatus;
import com.healthcare.backend.patient.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final HospitalRepository hospitalRepository;

    @Transactional
    public PatientResponse createPatient(CreatePatientRequest request, UserPrincipal principal) {
        verifyPatientManagementPermission(principal);

        Long hospitalId = getRequiredHospitalId(principal);
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Hospital not found with ID: " + hospitalId));

        // Duplicate mobile check within same hospital
        if (patientRepository.existsByMobileNumberAndHospitalId(request.getMobileNumber(), hospitalId)) {
            throw new IllegalStateException("Patient with mobile number '" + request.getMobileNumber() + "' already exists in this hospital");
        }

        // Generate deterministic per-hospital patientCode (e.g. CITY001-P00001)
        long patientCount = patientRepository.countByHospitalId(hospitalId);
        String hospitalPrefix = (hospital.getCode() != null && !hospital.getCode().isBlank())
                ? hospital.getCode()
                : "HOSP" + hospitalId;
        String patientCode = String.format("%s-P%05d", hospitalPrefix, patientCount + 1);

        Patient patient = new Patient();
        patient.setPatientCode(patientCode);
        patient.setFirstName(request.getFirstName());
        patient.setLastName(request.getLastName());
        patient.setDateOfBirth(request.getDateOfBirth());
        patient.setGender(request.getGender());
        patient.setMobileNumber(request.getMobileNumber());
        patient.setEmail(request.getEmail());
        patient.setAddress(request.getAddress());
        patient.setCity(request.getCity());
        patient.setState(request.getState());
        patient.setPincode(request.getPincode());
        patient.setEmergencyContactName(request.getEmergencyContactName());
        patient.setEmergencyContactNumber(request.getEmergencyContactNumber());
        patient.setBloodGroup(request.getBloodGroup());
        patient.setStatus(PatientStatus.ACTIVE);
        patient.setHospital(hospital);

        Patient saved = patientRepository.save(patient);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public PatientResponse getPatientById(Long id, UserPrincipal principal) {
        verifyPatientManagementPermission(principal);
        Long hospitalId = getRequiredHospitalId(principal);

        Patient patient = patientRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Patient not found with ID: " + id));

        return mapToResponse(patient);
    }

    @Transactional(readOnly = true)
    public Page<PatientResponse> getPatients(Pageable pageable, UserPrincipal principal) {
        verifyPatientManagementPermission(principal);
        Long hospitalId = getRequiredHospitalId(principal);

        return patientRepository.findByHospitalId(hospitalId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    public PatientResponse updatePatient(Long id, UpdatePatientRequest request, UserPrincipal principal) {
        verifyPatientManagementPermission(principal);
        Long hospitalId = getRequiredHospitalId(principal);

        Patient patient = patientRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Patient not found with ID: " + id));

        // If mobile number is changing, check uniqueness within hospital
        if (request.getMobileNumber() != null && !request.getMobileNumber().isBlank()
                && !request.getMobileNumber().equals(patient.getMobileNumber())) {
            if (patientRepository.existsByMobileNumberAndHospitalId(request.getMobileNumber(), hospitalId)) {
                throw new IllegalStateException("Patient with mobile number '" + request.getMobileNumber() + "' already exists in this hospital");
            }
            patient.setMobileNumber(request.getMobileNumber());
        }

        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            patient.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            patient.setLastName(request.getLastName());
        }
        if (request.getDateOfBirth() != null) {
            patient.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getGender() != null) {
            patient.setGender(request.getGender());
        }
        if (request.getEmail() != null) {
            patient.setEmail(request.getEmail());
        }
        if (request.getAddress() != null) {
            patient.setAddress(request.getAddress());
        }
        if (request.getCity() != null) {
            patient.setCity(request.getCity());
        }
        if (request.getState() != null) {
            patient.setState(request.getState());
        }
        if (request.getPincode() != null) {
            patient.setPincode(request.getPincode());
        }
        if (request.getEmergencyContactName() != null) {
            patient.setEmergencyContactName(request.getEmergencyContactName());
        }
        if (request.getEmergencyContactNumber() != null) {
            patient.setEmergencyContactNumber(request.getEmergencyContactNumber());
        }
        if (request.getBloodGroup() != null) {
            patient.setBloodGroup(request.getBloodGroup());
        }
        if (request.getStatus() != null) {
            patient.setStatus(request.getStatus());
        }

        Patient updated = patientRepository.save(patient);
        return mapToResponse(updated);
    }

    private void verifyPatientManagementPermission(UserPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("User must be authenticated to perform patient operations");
        }
        Collection<? extends GrantedAuthority> authorities = principal.getAuthorities();
        boolean hasPermission = authorities.stream().anyMatch(auth ->
                auth.getAuthority().equals("ROLE_" + RoleName.DOCTOR.name()) ||
                auth.getAuthority().equals("ROLE_" + RoleName.HOSPITAL_ADMIN.name()) ||
                auth.getAuthority().equals("ROLE_" + RoleName.RECEPTIONIST.name()) ||
                auth.getAuthority().equals("ROLE_" + RoleName.PLATFORM_ADMIN.name())
        );

        if (!hasPermission) {
            throw new AccessDeniedException("User role does not have permission for patient management");
        }
    }

    private Long getRequiredHospitalId(UserPrincipal principal) {
        if (principal == null || principal.getHospitalId() == null) {
            throw new IllegalStateException("Authenticated user is not associated with any hospital");
        }
        return principal.getHospitalId();
    }

    private PatientResponse mapToResponse(Patient patient) {
        return PatientResponse.builder()
                .id(patient.getId())
                .patientCode(patient.getPatientCode())
                .firstName(patient.getFirstName())
                .lastName(patient.getLastName())
                .dateOfBirth(patient.getDateOfBirth())
                .gender(patient.getGender())
                .mobileNumber(patient.getMobileNumber())
                .email(patient.getEmail())
                .address(patient.getAddress())
                .city(patient.getCity())
                .state(patient.getState())
                .pincode(patient.getPincode())
                .emergencyContactName(patient.getEmergencyContactName())
                .emergencyContactNumber(patient.getEmergencyContactNumber())
                .bloodGroup(patient.getBloodGroup())
                .status(patient.getStatus())
                .hospitalId(patient.getHospital().getId())
                .createdAt(patient.getCreatedAt())
                .updatedAt(patient.getUpdatedAt())
                .build();
    }
}
