package com.healthcare.backend.consultation.controller;

import com.healthcare.backend.consultation.dto.ConsultationResponse;
import com.healthcare.backend.consultation.dto.CreateConsultationRequest;
import com.healthcare.backend.consultation.dto.UpdateConsultationRequest;
import com.healthcare.backend.consultation.service.ConsultationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ConsultationController {

    private final ConsultationService consultationService;

    @PostMapping("/consultations")
    @PreAuthorize("hasAnyRole('DOCTOR', 'HOSPITAL_ADMIN')")
    public ResponseEntity<ConsultationResponse> createConsultation(@Valid @RequestBody CreateConsultationRequest request) {
        ConsultationResponse response = consultationService.createConsultation(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/consultations/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'HOSPITAL_ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<ConsultationResponse> getConsultationById(@PathVariable Long id) {
        ConsultationResponse response = consultationService.getConsultationById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/consultations")
    @PreAuthorize("hasAnyRole('DOCTOR', 'HOSPITAL_ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<Page<ConsultationResponse>> getConsultations(
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "consultationDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ConsultationResponse> response = consultationService.getConsultations(doctorId, patientId, status, date, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/doctors/me/consultations")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Page<ConsultationResponse>> getDoctorOwnConsultations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("consultationDate").descending());
        Page<ConsultationResponse> response = consultationService.getDoctorOwnConsultations(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/patients/{patientId}/consultations")
    @PreAuthorize("hasAnyRole('DOCTOR', 'HOSPITAL_ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<Page<ConsultationResponse>> getPatientConsultations(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("consultationDate").descending());
        Page<ConsultationResponse> response = consultationService.getPatientConsultations(patientId, pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/consultations/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'HOSPITAL_ADMIN')")
    public ResponseEntity<ConsultationResponse> updateConsultation(
            @PathVariable Long id,
            @RequestBody UpdateConsultationRequest request
    ) {
        ConsultationResponse response = consultationService.updateConsultation(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/consultations/{id}/complete")
    @PreAuthorize("hasAnyRole('DOCTOR', 'HOSPITAL_ADMIN')")
    public ResponseEntity<ConsultationResponse> completeConsultation(@PathVariable Long id) {
        ConsultationResponse response = consultationService.completeConsultation(id);
        return ResponseEntity.ok(response);
    }
}
