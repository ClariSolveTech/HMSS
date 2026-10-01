package com.healthcare.backend.lab.controller;

import com.healthcare.backend.lab.dto.CreateLabTestRequest;
import com.healthcare.backend.lab.dto.LabTestResponse;
import com.healthcare.backend.lab.dto.UpdateLabTestRequest;
import com.healthcare.backend.lab.service.LabTestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lab/tests")
@RequiredArgsConstructor
public class LabTestController {

    private final LabTestService labTestService;

    @GetMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'HOSPITAL_ADMIN', 'RECEPTIONIST', 'LAB_STAFF', 'PLATFORM_ADMIN')")
    public ResponseEntity<List<LabTestResponse>> getLabTests(
            @RequestParam(required = false, defaultValue = "true") Boolean activeOnly
    ) {
        List<LabTestResponse> response = labTestService.getLabTests(activeOnly);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'HOSPITAL_ADMIN', 'RECEPTIONIST', 'LAB_STAFF', 'PLATFORM_ADMIN')")
    public ResponseEntity<LabTestResponse> getLabTestById(@PathVariable Long id) {
        LabTestResponse response = labTestService.getLabTestById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN', 'LAB_STAFF', 'PLATFORM_ADMIN')")
    public ResponseEntity<LabTestResponse> createLabTest(@Valid @RequestBody CreateLabTestRequest request) {
        LabTestResponse response = labTestService.createLabTest(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN', 'LAB_STAFF', 'PLATFORM_ADMIN')")
    public ResponseEntity<LabTestResponse> updateLabTest(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLabTestRequest request
    ) {
        LabTestResponse response = labTestService.updateLabTest(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN', 'LAB_STAFF', 'PLATFORM_ADMIN')")
    public ResponseEntity<LabTestResponse> toggleLabTestStatus(
            @PathVariable Long id,
            @RequestParam boolean active
    ) {
        LabTestResponse response = labTestService.toggleLabTestStatus(id, active);
        return ResponseEntity.ok(response);
    }
}
