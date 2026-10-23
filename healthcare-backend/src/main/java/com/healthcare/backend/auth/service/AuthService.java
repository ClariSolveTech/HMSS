package com.healthcare.backend.auth.service;

import com.healthcare.backend.auth.dto.AuthResponse;
import com.healthcare.backend.auth.dto.LoginRequest;
import com.healthcare.backend.auth.dto.RegisterRequest;
import com.healthcare.backend.auth.entity.Role;
import com.healthcare.backend.auth.entity.RoleName;
import com.healthcare.backend.auth.entity.User;
import com.healthcare.backend.auth.entity.UserStatus;
import com.healthcare.backend.auth.repository.RoleRepository;
import com.healthcare.backend.auth.repository.UserRepository;
import com.healthcare.backend.auth.security.JwtService;
import com.healthcare.backend.hospital.entity.Hospital;
import com.healthcare.backend.hospital.repository.HospitalRepository;
import com.healthcare.backend.auth.dto.PatientRegisterRequest;
import com.healthcare.backend.hospital.entity.HospitalStatus;
import com.healthcare.backend.patient.entity.BloodGroup;
import com.healthcare.backend.patient.entity.Patient;
import com.healthcare.backend.patient.entity.PatientStatus;
import com.healthcare.backend.patient.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import com.healthcare.backend.auth.dto.PatientActivateRequest;
import com.healthcare.backend.patient.entity.PatientActivationCode;
import com.healthcare.backend.patient.repository.PatientActivationCodeRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final HospitalRepository hospitalRepository;
    private final PatientRepository patientRepository;
    private final PatientActivationCodeRepository patientActivationCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 1. Restriction check: Public registration cannot create PLATFORM_ADMIN,
        // HOSPITAL_ADMIN, or PATIENT
        if (request.getRole() == RoleName.PLATFORM_ADMIN || request.getRole() == RoleName.HOSPITAL_ADMIN || request.getRole() == RoleName.PATIENT) {
            throw new IllegalArgumentException(
                    "Public registration does not allow administrative or patient roles (" + request.getRole() + ")");
        }

        // 2. Check duplicate email / mobile
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Email '" + request.getEmail() + "' is already registered");
        }
        if (userRepository.existsByMobileNumber(request.getMobileNumber())) {
            throw new IllegalStateException("Mobile number '" + request.getMobileNumber() + "' is already registered");
        }

        // 3. Verify hospital exists
        Hospital hospital = hospitalRepository.findById(request.getHospitalId())
                .orElseThrow(
                        () -> new NoSuchElementException("Hospital not found with ID: " + request.getHospitalId()));

        // 4. Find or auto-initialize Role
        Role role = roleRepository.findByName(request.getRole())
                .orElseGet(() -> roleRepository.save(new Role(request.getRole())));

        // 5. Create user with encoded password (never plaintext)
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setMobileNumber(request.getMobileNumber());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setHospital(hospital);
        user.setStatus(UserStatus.ACTIVE);
        user.getRoles().add(role);

        // 6. Save user
        User savedUser = userRepository.save(user);

        // 7. Generate JWT & return response (no password Hash exposed)
        String token = jwtService.generateToken(savedUser);

        Set<String> roleNames = savedUser.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .userId(savedUser.getId())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .roles(roleNames)
                .hospitalId(savedUser.getHospital().getId())
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // 1. Find user by email or mobile
        User user = userRepository.findByEmail(request.getEmailOrMobile())
                .orElseGet(() -> userRepository.findByMobileNumber(request.getEmailOrMobile())
                        .orElseThrow(() -> new BadCredentialsException("Invalid email/mobile or password")));

        // 2. Check status
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadCredentialsException("Account status is " + user.getStatus() + ". Login not permitted.");
        }

        // 3. Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email/mobile or password");
        }

        // 4. Generate token
        String token = jwtService.generateToken(user);

        Set<String> roleNames = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(roleNames)
                .hospitalId(user.getHospital().getId())
                .build();
    }

    @Transactional
    public AuthResponse registerPatient(PatientRegisterRequest request) {
        // 1. Verify hospital exists and is ACTIVE
        Hospital hospital = hospitalRepository.findById(request.getHospitalId())
                .orElseThrow(() -> new NoSuchElementException("Hospital not found with ID: " + request.getHospitalId()));
        if (hospital.getStatus() != HospitalStatus.ACTIVE) {
            throw new IllegalStateException("Hospital is not active for registration");
        }

        // 2. Check duplicate email / mobile in users
        if (userRepository.existsByMobileNumber(request.getMobileNumber())) {
            throw new IllegalStateException("Mobile number '" + request.getMobileNumber() + "' is already registered");
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Email '" + request.getEmail() + "' is already registered");
        }

        // 3. Check duplicate mobile in patients table for this hospital
        if (patientRepository.existsByMobileNumberAndHospitalId(request.getMobileNumber(), hospital.getId())) {
            throw new IllegalStateException("A patient record with mobile number '" + request.getMobileNumber() + "' already exists in this hospital. Please log in or contact reception.");
        }

        // 4. Find or auto-initialize Role (strictly PATIENT)
        Role patientRole = roleRepository.findByName(RoleName.PATIENT)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.PATIENT)));

        // 5. Create user with encoded password (never plaintext)
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setMobileNumber(request.getMobileNumber());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setHospital(hospital);
        user.setStatus(UserStatus.ACTIVE);
        user.getRoles().add(patientRole);

        User savedUser = userRepository.save(user);

        // 6. Generate deterministic per-hospital patientCode (e.g. CCH001-P00001)
        long patientCount = patientRepository.countByHospitalId(hospital.getId());
        String hospitalPrefix = (hospital.getCode() != null && !hospital.getCode().isBlank())
                ? hospital.getCode()
                : "HOSP" + hospital.getId();
        String patientCode = String.format("%s-P%05d", hospitalPrefix, patientCount + 1);

        // 7. Create linked Patient clinical record
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
        patient.setBloodGroup(request.getBloodGroup() != null ? request.getBloodGroup() : BloodGroup.UNKNOWN);
        patient.setStatus(PatientStatus.ACTIVE);
        patient.setHospital(hospital);
        patient.setUser(savedUser);

        patientRepository.save(patient);

        // 8. Generate JWT & return response (safe DTO with no credentials or hashes)
        String token = jwtService.generateToken(savedUser);
        Set<String> roleNames = savedUser.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .userId(savedUser.getId())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .roles(roleNames)
                .hospitalId(savedUser.getHospital().getId())
                .build();
    }

    @Transactional
    public AuthResponse activatePatient(PatientActivateRequest request) {
        String normalizedCode = request.getActivationCode().trim().toUpperCase();
        String codeHash = com.healthcare.backend.patient.service.PatientService.hashActivationCode(normalizedCode);
        LocalDateTime now = LocalDateTime.now();

        PatientActivationCode activationCode = patientActivationCodeRepository
                .findByCodeHashAndUsedAtIsNullAndExpiresAtAfter(codeHash, now)
                .orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException(
                        "Invalid or expired activation code. Please verify the code or contact hospital reception."));

        Patient patient = activationCode.getPatient();
        if (patient.getUser() != null) {
            activationCode.setUsedAt(now);
            patientActivationCodeRepository.save(activationCode);
            throw new IllegalStateException("This patient record is already linked to an active user account. Please log in directly.");
        }

        String email = request.getEmail() != null && !request.getEmail().isBlank()
                ? request.getEmail().trim().toLowerCase()
                : patient.getEmail();

        if (userRepository.existsByMobileNumber(patient.getMobileNumber())) {
            throw new IllegalStateException("A user account with mobile number '" + patient.getMobileNumber() + "' already exists. Please contact reception.");
        }
        if (email != null && !email.isBlank() && userRepository.existsByEmail(email)) {
            throw new IllegalStateException("A user account with email '" + email + "' already exists.");
        }

        Role patientRole = roleRepository.findByName(RoleName.PATIENT)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.PATIENT)));

        User user = new User();
        user.setFirstName(patient.getFirstName());
        user.setLastName(patient.getLastName());
        user.setMobileNumber(patient.getMobileNumber());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setHospital(patient.getHospital());
        user.setStatus(com.healthcare.backend.auth.entity.UserStatus.ACTIVE);
        user.setRoles(Set.of(patientRole));

        User savedUser = userRepository.save(user);

        patient.setUser(savedUser);
        if (email != null && !email.isBlank() && (patient.getEmail() == null || patient.getEmail().isBlank())) {
            patient.setEmail(email);
        }
        patientRepository.save(patient);

        activationCode.setUsedAt(now);
        patientActivationCodeRepository.save(activationCode);

        String token = jwtService.generateToken(savedUser);
        Set<String> roleNames = Set.of(RoleName.PATIENT.name());

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .userId(savedUser.getId())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .roles(roleNames)
                .hospitalId(savedUser.getHospital().getId())
                .build();
    }
}
