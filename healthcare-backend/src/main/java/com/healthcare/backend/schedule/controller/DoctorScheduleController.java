package com.healthcare.backend.schedule.controller;

import com.healthcare.backend.schedule.dto.*;
import com.healthcare.backend.schedule.entity.ScheduleStatus;
import com.healthcare.backend.schedule.service.DoctorScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DoctorScheduleController {

    private final DoctorScheduleService doctorScheduleService;

    @PostMapping("/doctor-schedules")
    public ResponseEntity<DoctorScheduleResponse> createSchedule(@Valid @RequestBody CreateDoctorScheduleRequest request) {
        DoctorScheduleResponse response = doctorScheduleService.createSchedule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/doctor-schedules/{id}")
    public ResponseEntity<DoctorScheduleResponse> getScheduleById(@PathVariable Long id) {
        DoctorScheduleResponse response = doctorScheduleService.getScheduleById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/doctor-schedules")
    public ResponseEntity<Page<DoctorScheduleResponse>> getSchedules(
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) DayOfWeek dayOfWeek,
            @RequestParam(required = false) ScheduleStatus status,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        Page<DoctorScheduleResponse> page = doctorScheduleService.getSchedules(doctorId, dayOfWeek, status, pageable);
        return ResponseEntity.ok(page);
    }

    @PutMapping("/doctor-schedules/{id}")
    public ResponseEntity<DoctorScheduleResponse> updateSchedule(
            @PathVariable Long id,
            @RequestBody UpdateDoctorScheduleRequest request) {
        DoctorScheduleResponse response = doctorScheduleService.updateSchedule(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/doctor-schedules/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long id) {
        doctorScheduleService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/doctors/me/schedules")
    public ResponseEntity<List<DoctorScheduleResponse>> getMySchedules() {
        List<DoctorScheduleResponse> schedules = doctorScheduleService.getDoctorSchedulesForCurrentUser();
        return ResponseEntity.ok(schedules);
    }

    @GetMapping("/doctors/{doctorId}/availability")
    public ResponseEntity<DoctorAvailabilityResponse> getDoctorAvailability(
            @PathVariable Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        DoctorAvailabilityResponse response = doctorScheduleService.getDoctorAvailability(doctorId, date);
        return ResponseEntity.ok(response);
    }
}
