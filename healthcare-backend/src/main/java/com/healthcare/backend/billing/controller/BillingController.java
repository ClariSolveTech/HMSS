package com.healthcare.backend.billing.controller;

import com.healthcare.backend.billing.dto.*;
import com.healthcare.backend.billing.service.BillingService;
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
public class BillingController {

    private final BillingService billingService;

    @PostMapping("/invoices")
    @PreAuthorize("hasAnyRole('DOCTOR', 'HOSPITAL_ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<InvoiceResponse> createInvoice(@Valid @RequestBody CreateInvoiceRequest request) {
        InvoiceResponse response = billingService.createInvoice(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/invoices/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'HOSPITAL_ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<InvoiceResponse> getInvoiceById(@PathVariable Long id) {
        InvoiceResponse response = billingService.getInvoiceById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/invoices/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'HOSPITAL_ADMIN')")
    public ResponseEntity<InvoiceResponse> updateInvoice(
            @PathVariable Long id,
            @Valid @RequestBody UpdateInvoiceRequest request
    ) {
        InvoiceResponse response = billingService.updateInvoice(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/invoices/{id}/issue")
    @PreAuthorize("hasAnyRole('DOCTOR', 'HOSPITAL_ADMIN')")
    public ResponseEntity<InvoiceResponse> issueInvoice(@PathVariable Long id) {
        InvoiceResponse response = billingService.issueInvoice(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/invoices/{id}/cancel")
    @PreAuthorize("hasAnyRole('DOCTOR', 'HOSPITAL_ADMIN')")
    public ResponseEntity<InvoiceResponse> cancelInvoice(@PathVariable Long id) {
        InvoiceResponse response = billingService.cancelInvoice(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/invoices/{id}/payments")
    @PreAuthorize("hasAnyRole('DOCTOR', 'HOSPITAL_ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<PaymentResponse> addPayment(
            @PathVariable Long id,
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        PaymentResponse response = billingService.addPayment(id, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/invoices/{id}/payments")
    @PreAuthorize("hasAnyRole('DOCTOR', 'HOSPITAL_ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByInvoiceId(@PathVariable Long id) {
        List<PaymentResponse> responses = billingService.getPaymentsByInvoiceId(id);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/patients/{patientId}/invoices")
    @PreAuthorize("hasAnyRole('DOCTOR', 'HOSPITAL_ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<Page<InvoiceResponse>> getPatientInvoices(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<InvoiceResponse> response = billingService.getPatientInvoices(patientId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/doctors/me/invoices")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Page<InvoiceResponse>> getDoctorOwnInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<InvoiceResponse> response = billingService.getDoctorOwnInvoices(pageable);
        return ResponseEntity.ok(response);
    }
}
