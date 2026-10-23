package com.healthcare.backend.patientportal.controller;

import com.healthcare.backend.appointment.entity.AppointmentStatus;
import com.healthcare.backend.appointment.entity.AppointmentType;
import com.healthcare.backend.patientportal.dto.*;
import com.healthcare.backend.patientportal.service.PatientPortalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/patient-portal")
@PreAuthorize("hasRole('PATIENT')")
@RequiredArgsConstructor
public class PatientPortalController {

    private final PatientPortalService patientPortalService;

    @GetMapping("/dashboard")
    public ResponseEntity<PatientDashboardSummaryResponse> getDashboardSummary() {
        PatientDashboardSummaryResponse response = patientPortalService.getDashboardSummary();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/appointments")
    public ResponseEntity<Page<PatientAppointmentSummary>> getAppointments(
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) AppointmentType appointmentType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "appointmentDate,desc") String[] sort
    ) {
        Pageable pageable = createPageable(page, size, sort);
        Page<PatientAppointmentSummary> response = patientPortalService.getAppointments(status, appointmentType, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/appointments/{id}")
    public ResponseEntity<PatientAppointmentDetailResponse> getAppointmentDetail(@PathVariable Long id) {
        PatientAppointmentDetailResponse response = patientPortalService.getAppointmentDetail(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/appointments/{id}/teleconsult-access")
    public ResponseEntity<TeleconsultAccessResponse> getTeleconsultAccess(@PathVariable Long id) {
        TeleconsultAccessResponse response = patientPortalService.getTeleconsultAccess(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile")
    public ResponseEntity<PatientProfileResponse> getProfile() {
        PatientProfileResponse response = patientPortalService.getProfile();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    public ResponseEntity<PatientProfileResponse> updateProfile(@Valid @RequestBody UpdatePatientProfileRequest request) {
        PatientProfileResponse response = patientPortalService.updateProfile(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/prescriptions")
    public ResponseEntity<Page<PatientPrescriptionSummaryResponse>> getPrescriptions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort
    ) {
        Pageable pageable = createPageable(page, size, sort);
        Page<PatientPrescriptionSummaryResponse> response = patientPortalService.getPrescriptions(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/prescriptions/{id}")
    public ResponseEntity<PatientPrescriptionDetailResponse> getPrescriptionDetail(@PathVariable Long id) {
        PatientPrescriptionDetailResponse response = patientPortalService.getPrescriptionDetail(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/consultations")
    public ResponseEntity<Page<PatientConsultationSummaryResponse>> getConsultations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PatientConsultationSummaryResponse> response = patientPortalService.getConsultations(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/lab-orders")
    public ResponseEntity<Page<PatientLabOrderSummaryResponse>> getLabOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "orderedAt,desc") String[] sort
    ) {
        Pageable pageable = createPageable(page, size, sort);
        Page<PatientLabOrderSummaryResponse> response = patientPortalService.getLabOrders(pageable);
        return ResponseEntity.ok(response);
    }

    private Pageable createPageable(int page, int size, String[] sort) {
        if (sort == null || sort.length == 0) {
            return PageRequest.of(page, size);
        }
        if (sort.length == 1 && sort[0].contains(",")) {
            sort = sort[0].split(",");
        }
        if (sort.length == 2 && (sort[1].equalsIgnoreCase("asc") || sort[1].equalsIgnoreCase("desc"))) {
            Sort.Direction direction = sort[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
            return PageRequest.of(page, size, Sort.by(direction, sort[0]));
        }
        return PageRequest.of(page, size, Sort.by(sort));
    }
}
