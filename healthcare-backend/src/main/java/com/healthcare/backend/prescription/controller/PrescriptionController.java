package com.healthcare.backend.prescription.controller;

import com.healthcare.backend.prescription.dto.CreatePrescriptionRequest;
import com.healthcare.backend.prescription.dto.PrescriptionResponse;
import com.healthcare.backend.prescription.dto.UpdatePrescriptionRequest;
import com.healthcare.backend.prescription.service.PrescriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @PostMapping("/prescriptions")
    @PreAuthorize("hasAnyRole('DOCTOR', 'HOSPITAL_ADMIN')")
    public ResponseEntity<PrescriptionResponse> createPrescription(@Valid @RequestBody CreatePrescriptionRequest request) {
        PrescriptionResponse response = prescriptionService.createPrescription(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/prescriptions/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'HOSPITAL_ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<PrescriptionResponse> getPrescriptionById(@PathVariable Long id) {
        PrescriptionResponse response = prescriptionService.getPrescriptionById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/prescriptions/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'HOSPITAL_ADMIN')")
    public ResponseEntity<PrescriptionResponse> updatePrescription(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePrescriptionRequest request
    ) {
        PrescriptionResponse response = prescriptionService.updatePrescription(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/prescriptions/{id}/issue")
    @PreAuthorize("hasAnyRole('DOCTOR', 'HOSPITAL_ADMIN')")
    public ResponseEntity<PrescriptionResponse> issuePrescription(@PathVariable Long id) {
        PrescriptionResponse response = prescriptionService.issuePrescription(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/prescriptions/{id}/cancel")
    @PreAuthorize("hasAnyRole('DOCTOR', 'HOSPITAL_ADMIN')")
    public ResponseEntity<PrescriptionResponse> cancelPrescription(@PathVariable Long id) {
        PrescriptionResponse response = prescriptionService.cancelPrescription(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/doctors/me/prescriptions")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Page<PrescriptionResponse>> getDoctorOwnPrescriptions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PrescriptionResponse> response = prescriptionService.getDoctorOwnPrescriptions(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/patients/{patientId}/prescriptions")
    @PreAuthorize("hasAnyRole('DOCTOR', 'HOSPITAL_ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<Page<PrescriptionResponse>> getPatientPrescriptions(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PrescriptionResponse> response = prescriptionService.getPatientPrescriptions(patientId, pageable);
        return ResponseEntity.ok(response);
    }
}
