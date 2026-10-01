package com.healthcare.backend.lab.controller;

import com.healthcare.backend.lab.dto.CreateLabResultRequest;
import com.healthcare.backend.lab.dto.LabResultResponse;
import com.healthcare.backend.lab.dto.ReviewLabResultRequest;
import com.healthcare.backend.lab.entity.LabResultStatus;
import com.healthcare.backend.lab.service.LabResultService;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class LabResultController {

    private final LabResultService labResultService;

    @PostMapping("/lab/results")
    @PreAuthorize("hasAnyRole('LAB_STAFF', 'HOSPITAL_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<LabResultResponse> createLabResult(@Valid @RequestBody CreateLabResultRequest request) {
        LabResultResponse response = labResultService.createLabResult(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/lab/results/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'LAB_STAFF', 'HOSPITAL_ADMIN')")
    public ResponseEntity<LabResultResponse> getLabResultById(@PathVariable Long id) {
        LabResultResponse response = labResultService.getLabResultById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/lab/orders/{orderId}/results")
    @PreAuthorize("hasAnyRole('DOCTOR', 'LAB_STAFF', 'HOSPITAL_ADMIN')")
    public ResponseEntity<List<LabResultResponse>> getResultsByLabOrder(@PathVariable Long orderId) {
        List<LabResultResponse> response = labResultService.getResultsByLabOrder(orderId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/doctors/me/lab-results")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Page<LabResultResponse>> getDoctorLabResults(
            @RequestParam(required = false) LabResultStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<LabResultResponse> response = labResultService.getDoctorLabResults(status, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/patients/{patientId}/lab-results")
    @PreAuthorize("hasAnyRole('DOCTOR', 'LAB_STAFF', 'HOSPITAL_ADMIN')")
    public ResponseEntity<Page<LabResultResponse>> getPatientLabResults(
            @PathVariable Long patientId,
            @RequestParam(required = false) LabResultStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<LabResultResponse> response = labResultService.getPatientLabResults(patientId, status, pageable);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/lab/results/{id}/review")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<LabResultResponse> reviewLabResult(
            @PathVariable Long id,
            @RequestBody(required = false) ReviewLabResultRequest request
    ) {
        LabResultResponse response = labResultService.reviewLabResult(id, request);
        return ResponseEntity.ok(response);
    }
}
