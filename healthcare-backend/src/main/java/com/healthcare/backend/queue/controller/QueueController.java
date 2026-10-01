package com.healthcare.backend.queue.controller;

import com.healthcare.backend.queue.dto.*;
import com.healthcare.backend.queue.entity.DoctorQueueStatus;
import com.healthcare.backend.queue.service.QueueService;
import jakarta.validation.Valid;
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
public class QueueController {

    private final QueueService queueService;

    public QueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    // 1. Create walk-in or manual queue entry
    @PostMapping("/queue")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'DOCTOR', 'HOSPITAL_ADMIN')")
    public ResponseEntity<QueueEntryResponse> createQueueEntry(@Valid @RequestBody CreateQueueEntryRequest request) {
        QueueEntryResponse response = queueService.createQueueEntry(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // 2. Check in appointment into queue
    @PostMapping("/queue/check-in/{appointmentId}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'DOCTOR', 'HOSPITAL_ADMIN')")
    public ResponseEntity<QueueEntryResponse> checkInAppointment(@PathVariable Long appointmentId) {
        QueueEntryResponse response = queueService.checkInAppointment(appointmentId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // 3. Get hospital queue entries with filtering and pagination
    @GetMapping("/queue")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'DOCTOR', 'HOSPITAL_ADMIN')")
    public ResponseEntity<Page<QueueEntryResponse>> getQueueEntries(
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "tokenNumber,asc") String sort
    ) {
        String[] sortParts = sort.split(",");
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParts[0]));

        Page<QueueEntryResponse> entries = queueService.getQueueEntries(doctorId, date, status, priority, pageable);
        return ResponseEntity.ok(entries);
    }

    // 4. Get single queue entry by ID
    @GetMapping("/queue/{id}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'DOCTOR', 'HOSPITAL_ADMIN')")
    public ResponseEntity<QueueEntryResponse> getQueueEntryById(@PathVariable Long id) {
        QueueEntryResponse response = queueService.getQueueEntryById(id);
        return ResponseEntity.ok(response);
    }

    // 5. Call patient (WAITING -> CALLED)
    @PatchMapping("/queue/{id}/call")
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECEPTIONIST', 'HOSPITAL_ADMIN')")
    public ResponseEntity<QueueEntryResponse> callPatient(@PathVariable Long id) {
        QueueEntryResponse response = queueService.callPatient(id);
        return ResponseEntity.ok(response);
    }

    // 6. Start consultation (CALLED/WAITING -> IN_CONSULTATION)
    @PatchMapping("/queue/{id}/start-consultation")
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECEPTIONIST', 'HOSPITAL_ADMIN')")
    public ResponseEntity<QueueEntryResponse> startConsultation(@PathVariable Long id) {
        QueueEntryResponse response = queueService.startConsultation(id);
        return ResponseEntity.ok(response);
    }

    // 7. Complete consultation (IN_CONSULTATION -> COMPLETED)
    @PatchMapping("/queue/{id}/complete")
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECEPTIONIST', 'HOSPITAL_ADMIN')")
    public ResponseEntity<QueueEntryResponse> completeConsultation(@PathVariable Long id) {
        QueueEntryResponse response = queueService.completeConsultation(id);
        return ResponseEntity.ok(response);
    }

    // 8. Skip patient (WAITING/CALLED -> SKIPPED)
    @PatchMapping("/queue/{id}/skip")
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECEPTIONIST', 'HOSPITAL_ADMIN')")
    public ResponseEntity<QueueEntryResponse> skipPatient(
            @PathVariable Long id,
            @RequestBody(required = false) UpdateQueueStatusRequest request
    ) {
        String reason = request != null ? request.getReason() : "Patient not present";
        QueueEntryResponse response = queueService.skipPatient(id, reason);
        return ResponseEntity.ok(response);
    }

    // 9. Recall skipped patient (SKIPPED -> WAITING)
    @PatchMapping("/queue/{id}/recall")
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECEPTIONIST', 'HOSPITAL_ADMIN')")
    public ResponseEntity<QueueEntryResponse> recallPatient(@PathVariable Long id) {
        QueueEntryResponse response = queueService.recallPatient(id);
        return ResponseEntity.ok(response);
    }

    // 10. Cancel queue entry (WAITING/CALLED -> CANCELLED)
    @PatchMapping("/queue/{id}/cancel")
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECEPTIONIST', 'HOSPITAL_ADMIN')")
    public ResponseEntity<QueueEntryResponse> cancelQueueEntry(
            @PathVariable Long id,
            @RequestBody(required = false) UpdateQueueStatusRequest request
    ) {
        String reason = request != null ? request.getReason() : "Queue entry cancelled";
        QueueEntryResponse response = queueService.cancelQueueEntry(id, reason);
        return ResponseEntity.ok(response);
    }

    // 11. Logged-in doctor's own queue
    @GetMapping("/doctors/me/queue")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Page<QueueEntryResponse>> getDoctorOwnQueue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<QueueEntryResponse> response = queueService.getDoctorOwnQueue(date, pageable);
        return ResponseEntity.ok(response);
    }

    // 12. Logged-in doctor's current active consultation
    @GetMapping("/doctors/me/queue/current")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<CurrentTokenResponse> getDoctorOwnCurrentConsultation() {
        CurrentTokenResponse response = queueService.getDoctorOwnCurrentConsultation();
        return ResponseEntity.ok(response);
    }

    // 13. Move next waiting patient into consultation for logged-in doctor
    @PostMapping("/doctors/me/queue/next")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<QueueEntryResponse> callNextPatientForDoctor() {
        QueueEntryResponse response = queueService.callNextPatientForDoctor();
        return ResponseEntity.ok(response);
    }

    // 14. Doctor updates own queue arrival status
    @PatchMapping("/doctors/me/queue/status")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<DoctorQueueStatusResponse> updateDoctorOwnQueueStatus(
            @Valid @RequestBody UpdateDoctorQueueStatusRequest request
    ) {
        DoctorQueueStatusResponse response = queueService.updateDoctorOwnQueueStatus(request.getStatus());
        return ResponseEntity.ok(response);
    }

    // 15. View doctor arrival status
    @GetMapping("/doctors/{doctorId}/queue/status")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'DOCTOR', 'HOSPITAL_ADMIN')")
    public ResponseEntity<DoctorQueueStatusResponse> getDoctorQueueStatus(@PathVariable Long doctorId) {
        DoctorQueueStatusResponse response = queueService.getDoctorQueueStatus(doctorId);
        return ResponseEntity.ok(response);
    }
}
