package com.healthcare.backend.doctor.service;

import com.healthcare.backend.auth.entity.RoleName;
import com.healthcare.backend.auth.entity.User;
import com.healthcare.backend.auth.repository.UserRepository;
import com.healthcare.backend.auth.security.SecurityUtils;
import com.healthcare.backend.auth.security.UserPrincipal;
import com.healthcare.backend.doctor.dto.CreateDoctorRequest;
import com.healthcare.backend.doctor.dto.DoctorResponse;
import com.healthcare.backend.doctor.dto.UpdateDoctorRequest;
import com.healthcare.backend.doctor.entity.Doctor;
import com.healthcare.backend.doctor.entity.DoctorStatus;
import com.healthcare.backend.doctor.repository.DoctorRepository;
import com.healthcare.backend.hospital.entity.Hospital;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;

    @Transactional
    public DoctorResponse createDoctor(CreateDoctorRequest request) {
        UserPrincipal currentUser = SecurityUtils.getRequiredCurrentUserPrincipal();
        Long hospitalId = SecurityUtils.getCurrentHospitalId();

        verifyDoctorCreationPermission(currentUser);

        User targetUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NoSuchElementException("User not found with id: " + request.getUserId()));

        // Multi-tenant check: Target user must belong to the same hospital
        if (targetUser.getHospital() == null || !targetUser.getHospital().getId().equals(hospitalId)) {
            throw new NoSuchElementException("User not found in your hospital with id: " + request.getUserId());
        }

        // Target user must have DOCTOR role
        boolean hasDoctorRole = targetUser.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.DOCTOR);
        if (!hasDoctorRole) {
            throw new IllegalArgumentException("Target user does not have DOCTOR role");
        }

        // Check if doctor profile already exists for this user
        if (doctorRepository.existsByUserId(targetUser.getId())) {
            throw new IllegalStateException("Doctor profile already exists for user id: " + targetUser.getId());
        }

        Hospital hospital = targetUser.getHospital();
        long nextSeq = doctorRepository.countByHospitalId(hospitalId) + 1;
        String doctorCode = String.format("%s-D%05d", hospital.getCode(), nextSeq);

        Doctor doctor = new Doctor();
        doctor.setUser(targetUser);
        doctor.setHospital(hospital);
        doctor.setDoctorCode(doctorCode);
        doctor.setSpecialization(request.getSpecialization());
        doctor.setQualification(request.getQualification());
        doctor.setLicenseNumber(request.getLicenseNumber());
        doctor.setConsultationFee(request.getConsultationFee());
        doctor.setStatus(DoctorStatus.ACTIVE);

        Doctor savedDoctor = doctorRepository.save(doctor);
        return DoctorResponse.fromEntity(savedDoctor);
    }

    @Transactional(readOnly = true)
    public DoctorResponse getDoctorById(Long id) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();

        Doctor doctor = doctorRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Doctor not found with id: " + id));

        return DoctorResponse.fromEntity(doctor);
    }

    @Transactional(readOnly = true)
    public DoctorResponse getCurrentDoctorProfile() {
        UserPrincipal currentUser = SecurityUtils.getRequiredCurrentUserPrincipal();
        Long hospitalId = SecurityUtils.getCurrentHospitalId();

        Doctor doctor = doctorRepository.findByUserIdAndHospitalId(currentUser.getUserId(), hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Doctor profile not found for current user"));

        return DoctorResponse.fromEntity(doctor);
    }

    @Transactional(readOnly = true)
    public Page<DoctorResponse> getDoctors(String specialization, DoctorStatus status, Pageable pageable) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();

        Page<Doctor> page;
        boolean hasSpec = specialization != null && !specialization.trim().isEmpty();
        boolean hasStatus = status != null;

        if (hasSpec && hasStatus) {
            page = doctorRepository.findByHospitalIdAndSpecializationContainingIgnoreCaseAndStatus(
                    hospitalId, specialization.trim(), status, pageable);
        } else if (hasSpec) {
            page = doctorRepository.findByHospitalIdAndSpecializationContainingIgnoreCase(
                    hospitalId, specialization.trim(), pageable);
        } else if (hasStatus) {
            page = doctorRepository.findByHospitalIdAndStatus(hospitalId, status, pageable);
        } else {
            page = doctorRepository.findByHospitalId(hospitalId, pageable);
        }

        return page.map(DoctorResponse::fromEntity);
    }

    @Transactional
    public DoctorResponse updateDoctor(Long id, UpdateDoctorRequest request) {
        UserPrincipal currentUser = SecurityUtils.getRequiredCurrentUserPrincipal();
        Long hospitalId = SecurityUtils.getCurrentHospitalId();

        Doctor doctor = doctorRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Doctor not found with id: " + id));

        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_HOSPITAL_ADMIN") || a.getAuthority().equals("ROLE_PLATFORM_ADMIN"));
        boolean isSelf = doctor.getUser().getId().equals(currentUser.getUserId());

        if (!isAdmin && !isSelf) {
            throw new AccessDeniedException("You do not have permission to update this doctor profile");
        }

        if (request.getSpecialization() != null && !request.getSpecialization().trim().isEmpty()) {
            doctor.setSpecialization(request.getSpecialization().trim());
        }
        if (request.getQualification() != null && !request.getQualification().trim().isEmpty()) {
            doctor.setQualification(request.getQualification().trim());
        }
        if (request.getLicenseNumber() != null && !request.getLicenseNumber().trim().isEmpty()) {
            doctor.setLicenseNumber(request.getLicenseNumber().trim());
        }
        if (request.getConsultationFee() != null) {
            doctor.setConsultationFee(request.getConsultationFee());
        }
        if (request.getStatus() != null && isAdmin) {
            doctor.setStatus(request.getStatus());
        }

        Doctor updatedDoctor = doctorRepository.save(doctor);
        return DoctorResponse.fromEntity(updatedDoctor);
    }

    private void verifyDoctorCreationPermission(UserPrincipal userPrincipal) {
        boolean isAuthorized = userPrincipal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_HOSPITAL_ADMIN") || a.getAuthority().equals("ROLE_PLATFORM_ADMIN"));

        if (!isAuthorized) {
            throw new AccessDeniedException("Only HOSPITAL_ADMIN or PLATFORM_ADMIN can create doctor profiles");
        }
    }
}
