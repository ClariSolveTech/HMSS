package com.healthcare.backend.appointment.controller;

import com.healthcare.backend.appointment.dto.AppointmentResponse;
import com.healthcare.backend.appointment.dto.CancelAppointmentRequest;
import com.healthcare.backend.appointment.dto.CreateAppointmentRequest;
import com.healthcare.backend.appointment.dto.RescheduleAppointmentRequest;
import com.healthcare.backend.appointment.entity.AppointmentStatus;
import com.healthcare.backend.appointment.entity.AppointmentType;
import com.healthcare.backend.appointment.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping("/appointments")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN', 'RECEPTIONIST', 'DOCTOR')")
    public ResponseEntity<AppointmentResponse> createAppointment(@Valid @RequestBody CreateAppointmentRequest request) {
        AppointmentResponse response = appointmentService.createAppointment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/appointments/{id}")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN', 'RECEPTIONIST', 'DOCTOR')")
    public ResponseEntity<AppointmentResponse> getAppointmentById(@PathVariable Long id) {
        AppointmentResponse response = appointmentService.getAppointmentById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/appointments")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN', 'RECEPTIONIST', 'DOCTOR')")
    public ResponseEntity<Page<AppointmentResponse>> getAppointments(
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) LocalDate appointmentDate,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) AppointmentType appointmentType,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "appointmentDate,asc") String[] sort
    ) {
        Pageable pageable = createPageable(page, size, sort);
        Page<AppointmentResponse> response = appointmentService.getAppointments(
                doctorId, patientId, appointmentDate, status, appointmentType, search, pageable
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/appointments/today")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN', 'RECEPTIONIST', 'DOCTOR')")
    public ResponseEntity<Page<AppointmentResponse>> getTodayAppointments(
            @RequestParam(required = false) Long doctorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "startTime,asc") String[] sort
    ) {
        Pageable pageable = createPageable(page, size, sort);
        Page<AppointmentResponse> response = appointmentService.getTodayAppointments(doctorId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/doctors/{doctorId}/appointments")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN', 'DOCTOR')")
    public ResponseEntity<Page<AppointmentResponse>> getDoctorAppointments(
            @PathVariable Long doctorId,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) AppointmentType appointmentType,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "appointmentDate,asc") String[] sort
    ) {
        Pageable pageable = createPageable(page, size, sort);
        Page<AppointmentResponse> response = appointmentService.getDoctorAppointments(doctorId, date, status, appointmentType, search, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/patients/{patientId}/appointments")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN', 'RECEPTIONIST', 'DOCTOR')")
    public ResponseEntity<Page<AppointmentResponse>> getPatientAppointments(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "appointmentDate,desc") String[] sort
    ) {
        Pageable pageable = createPageable(page, size, sort);
        Page<AppointmentResponse> response = appointmentService.getPatientAppointments(patientId, pageable);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/appointments/{id}/confirm")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN', 'RECEPTIONIST', 'DOCTOR')")
    public ResponseEntity<AppointmentResponse> confirmAppointment(@PathVariable Long id) {
        AppointmentResponse response = appointmentService.confirmAppointment(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/appointments/{id}/check-in")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN', 'RECEPTIONIST', 'DOCTOR')")
    public ResponseEntity<AppointmentResponse> checkInAppointment(@PathVariable Long id) {
        AppointmentResponse response = appointmentService.checkInAppointment(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/appointments/{id}/no-show")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN', 'RECEPTIONIST', 'DOCTOR')")
    public ResponseEntity<AppointmentResponse> markNoShowAppointment(@PathVariable Long id) {
        AppointmentResponse response = appointmentService.markNoShowAppointment(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/appointments/{id}/cancel")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN', 'RECEPTIONIST', 'DOCTOR')")
    public ResponseEntity<AppointmentResponse> cancelAppointment(
            @PathVariable Long id,
            @RequestBody(required = false) CancelAppointmentRequest request
    ) {
        String reason = request != null ? request.getReason() : "Cancelled by user";
        AppointmentResponse response = appointmentService.cancelAppointment(id, reason);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/appointments/{id}/reschedule")
    @PreAuthorize("hasAnyRole('HOSPITAL_ADMIN', 'RECEPTIONIST', 'DOCTOR')")
    public ResponseEntity<AppointmentResponse> rescheduleAppointment(
            @PathVariable Long id,
            @Valid @RequestBody RescheduleAppointmentRequest request
    ) {
        AppointmentResponse response = appointmentService.rescheduleAppointment(id, request);
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
