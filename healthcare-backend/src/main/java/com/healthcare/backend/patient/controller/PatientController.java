package com.healthcare.backend.patient.controller;

import com.healthcare.backend.auth.security.UserPrincipal;
import com.healthcare.backend.patient.dto.CreatePatientRequest;
import com.healthcare.backend.patient.dto.PatientResponse;
import com.healthcare.backend.patient.dto.UpdatePatientRequest;
import com.healthcare.backend.patient.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    @PostMapping
    public ResponseEntity<PatientResponse> createPatient(
            @Valid @RequestBody CreatePatientRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        PatientResponse response = patientService.createPatient(request, principal);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PatientResponse> getPatientById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        PatientResponse response = patientService.getPatientById(id, principal);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<PatientResponse>> getPatients(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Page<PatientResponse> response = patientService.getPatients(pageable, principal);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PatientResponse> updatePatient(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePatientRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        PatientResponse response = patientService.updatePatient(id, request, principal);
        return ResponseEntity.ok(response);
    }
}
