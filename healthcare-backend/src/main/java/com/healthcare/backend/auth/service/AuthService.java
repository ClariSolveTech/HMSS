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
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final HospitalRepository hospitalRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 1. Restriction check: Public registration cannot create PLATFORM_ADMIN or
        // HOSPITAL_ADMIN
        if (request.getRole() == RoleName.PLATFORM_ADMIN || request.getRole() == RoleName.HOSPITAL_ADMIN) {
            throw new IllegalArgumentException(
                    "Public registration does not allow administrative roles (" + request.getRole() + ")");
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
}
