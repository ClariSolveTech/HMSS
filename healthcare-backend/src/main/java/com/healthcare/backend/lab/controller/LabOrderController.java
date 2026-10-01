package com.healthcare.backend.lab.controller;

import com.healthcare.backend.lab.dto.*;
import com.healthcare.backend.lab.entity.LabOrderPriority;
import com.healthcare.backend.lab.entity.LabOrderStatus;
import com.healthcare.backend.lab.service.LabOrderService;
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
public class LabOrderController {

    private final LabOrderService labOrderService;

    @PostMapping("/lab/orders")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<LabOrderResponse> createLabOrder(@Valid @RequestBody CreateLabOrderRequest request) {
        LabOrderResponse response = labOrderService.createLabOrder(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/lab/orders/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'LAB_STAFF', 'HOSPITAL_ADMIN')")
    public ResponseEntity<LabOrderResponse> getLabOrderById(@PathVariable Long id) {
        LabOrderResponse response = labOrderService.getLabOrderById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/lab/orders")
    @PreAuthorize("hasAnyRole('DOCTOR', 'LAB_STAFF', 'HOSPITAL_ADMIN')")
    public ResponseEntity<Page<LabOrderResponse>> getHospitalLabOrders(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) LabOrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<LabOrderResponse> response = labOrderService.getHospitalLabOrders(patientId, doctorId, status, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/doctors/me/lab-orders")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Page<LabOrderResponse>> getDoctorLabOrders(
            @RequestParam(required = false) LabOrderStatus status,
            @RequestParam(required = false) LabOrderPriority priority,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<LabOrderResponse> response = labOrderService.getDoctorLabOrders(status, priority, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/patients/{patientId}/lab-orders")
    @PreAuthorize("hasAnyRole('DOCTOR', 'LAB_STAFF', 'HOSPITAL_ADMIN')")
    public ResponseEntity<Page<LabOrderResponse>> getPatientLabOrders(
            @PathVariable Long patientId,
            @RequestParam(required = false) LabOrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<LabOrderResponse> response = labOrderService.getPatientLabOrders(patientId, status, pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/lab/orders/{id}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<LabOrderResponse> updateLabOrder(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLabOrderRequest request
    ) {
        LabOrderResponse response = labOrderService.updateLabOrder(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/lab/orders/{id}/status")
    @PreAuthorize("hasAnyRole('DOCTOR', 'LAB_STAFF', 'HOSPITAL_ADMIN')")
    public ResponseEntity<LabOrderResponse> updateLabOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLabOrderStatusRequest request
    ) {
        LabOrderResponse response = labOrderService.updateLabOrderStatus(id, request.getStatus());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/lab/orders/{id}/cancel")
    @PreAuthorize("hasAnyRole('DOCTOR', 'HOSPITAL_ADMIN')")
    public ResponseEntity<LabOrderResponse> cancelLabOrder(@PathVariable Long id) {
        LabOrderResponse response = labOrderService.cancelLabOrder(id);
        return ResponseEntity.ok(response);
    }
}
