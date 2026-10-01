package com.healthcare.backend.schedule.service;

import com.healthcare.backend.auth.entity.RoleName;
import com.healthcare.backend.auth.security.SecurityUtils;
import com.healthcare.backend.auth.security.UserPrincipal;
import com.healthcare.backend.doctor.entity.Doctor;
import com.healthcare.backend.doctor.repository.DoctorRepository;
import com.healthcare.backend.hospital.entity.Hospital;
import com.healthcare.backend.schedule.dto.*;
import com.healthcare.backend.schedule.entity.DoctorSchedule;
import com.healthcare.backend.schedule.entity.ScheduleStatus;
import com.healthcare.backend.schedule.repository.DoctorScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class DoctorScheduleService {

    private final DoctorScheduleRepository doctorScheduleRepository;
    private final DoctorRepository doctorRepository;
    private final SlotGenerationService slotGenerationService;

    @Transactional
    public DoctorScheduleResponse createSchedule(CreateDoctorScheduleRequest request) {
        UserPrincipal currentUser = SecurityUtils.getRequiredCurrentUserPrincipal();
        Long hospitalId = SecurityUtils.getCurrentHospitalId();

        verifyAdminPermission(currentUser);

        Doctor doctor = doctorRepository.findByIdAndHospitalId(request.getDoctorId(), hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Doctor not found in your hospital with ID: " + request.getDoctorId()));

        validateTimeAndBreak(
                request.getStartTime(),
                request.getEndTime(),
                request.getSlotDurationMinutes(),
                request.getBreakStartTime(),
                request.getBreakEndTime()
        );

        // Check for overlapping active schedules
        List<DoctorSchedule> overlaps = doctorScheduleRepository.findOverlappingActiveSchedules(
                doctor.getId(), request.getDayOfWeek(), request.getStartTime(), request.getEndTime(), null
        );
        if (!overlaps.isEmpty()) {
            throw new IllegalStateException("Schedule time range overlaps with an existing active schedule for doctor ID: " + doctor.getId());
        }

        Hospital hospital = doctor.getHospital();

        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setDoctor(doctor);
        schedule.setHospital(hospital);
        schedule.setDayOfWeek(request.getDayOfWeek());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        schedule.setSlotDurationMinutes(request.getSlotDurationMinutes());
        schedule.setBreakStartTime(request.getBreakStartTime());
        schedule.setBreakEndTime(request.getBreakEndTime());
        schedule.setStatus(ScheduleStatus.ACTIVE);

        DoctorSchedule savedSchedule = doctorScheduleRepository.save(schedule);
        return DoctorScheduleResponse.fromEntity(savedSchedule);
    }

    @Transactional(readOnly = true)
    public DoctorScheduleResponse getScheduleById(Long id) {
        UserPrincipal currentUser = SecurityUtils.getRequiredCurrentUserPrincipal();
        Long hospitalId = SecurityUtils.getCurrentHospitalId();

        DoctorSchedule schedule = doctorScheduleRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Doctor schedule not found with ID: " + id));

        verifyViewPermission(currentUser, schedule);

        return DoctorScheduleResponse.fromEntity(schedule);
    }

    @Transactional(readOnly = true)
    public Page<DoctorScheduleResponse> getSchedules(Long doctorId, DayOfWeek dayOfWeek, ScheduleStatus status, Pageable pageable) {
        UserPrincipal currentUser = SecurityUtils.getRequiredCurrentUserPrincipal();
        Long hospitalId = SecurityUtils.getCurrentHospitalId();

        verifyAccessPermission(currentUser);

        Page<DoctorSchedule> page = doctorScheduleRepository.findByHospitalIdWithFilters(
                hospitalId, doctorId, dayOfWeek, status, pageable
        );

        return page.map(DoctorScheduleResponse::fromEntity);
    }

    @Transactional
    public DoctorScheduleResponse updateSchedule(Long id, UpdateDoctorScheduleRequest request) {
        UserPrincipal currentUser = SecurityUtils.getRequiredCurrentUserPrincipal();
        Long hospitalId = SecurityUtils.getCurrentHospitalId();

        verifyAdminPermission(currentUser);

        DoctorSchedule schedule = doctorScheduleRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Doctor schedule not found with ID: " + id));

        DayOfWeek newDay = request.getDayOfWeek() != null ? request.getDayOfWeek() : schedule.getDayOfWeek();
        LocalTime newStart = request.getStartTime() != null ? request.getStartTime() : schedule.getStartTime();
        LocalTime newEnd = request.getEndTime() != null ? request.getEndTime() : schedule.getEndTime();
        Integer newDuration = request.getSlotDurationMinutes() != null ? request.getSlotDurationMinutes() : schedule.getSlotDurationMinutes();
        LocalTime newBreakStart = request.getBreakStartTime() != null ? request.getBreakStartTime() : schedule.getBreakStartTime();
        LocalTime newBreakEnd = request.getBreakEndTime() != null ? request.getBreakEndTime() : schedule.getBreakEndTime();
        ScheduleStatus newStatus = request.getStatus() != null ? request.getStatus() : schedule.getStatus();

        validateTimeAndBreak(newStart, newEnd, newDuration, newBreakStart, newBreakEnd);

        if (newStatus == ScheduleStatus.ACTIVE) {
            List<DoctorSchedule> overlaps = doctorScheduleRepository.findOverlappingActiveSchedules(
                    schedule.getDoctor().getId(), newDay, newStart, newEnd, schedule.getId()
            );
            if (!overlaps.isEmpty()) {
                throw new IllegalStateException("Updated schedule time range overlaps with an existing active schedule");
            }
        }

        schedule.setDayOfWeek(newDay);
        schedule.setStartTime(newStart);
        schedule.setEndTime(newEnd);
        schedule.setSlotDurationMinutes(newDuration);
        schedule.setBreakStartTime(newBreakStart);
        schedule.setBreakEndTime(newBreakEnd);
        schedule.setStatus(newStatus);

        DoctorSchedule updatedSchedule = doctorScheduleRepository.save(schedule);
        return DoctorScheduleResponse.fromEntity(updatedSchedule);
    }

    @Transactional
    public void deleteSchedule(Long id) {
        UserPrincipal currentUser = SecurityUtils.getRequiredCurrentUserPrincipal();
        Long hospitalId = SecurityUtils.getCurrentHospitalId();

        verifyAdminPermission(currentUser);

        DoctorSchedule schedule = doctorScheduleRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Doctor schedule not found with ID: " + id));

        schedule.setStatus(ScheduleStatus.INACTIVE);
        doctorScheduleRepository.save(schedule);
    }

    @Transactional(readOnly = true)
    public List<DoctorScheduleResponse> getDoctorSchedulesForCurrentUser() {
        UserPrincipal currentUser = SecurityUtils.getRequiredCurrentUserPrincipal();
        Long hospitalId = SecurityUtils.getCurrentHospitalId();

        Doctor doctor = doctorRepository.findByUserIdAndHospitalId(currentUser.getUserId(), hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Doctor profile not found for authenticated user"));

        List<DoctorSchedule> schedules = doctorScheduleRepository.findByDoctorIdAndHospitalId(doctor.getId(), hospitalId);
        return schedules.stream().map(DoctorScheduleResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public DoctorAvailabilityResponse getDoctorAvailability(Long doctorId, LocalDate date) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();

        Doctor doctor = doctorRepository.findByIdAndHospitalId(doctorId, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Doctor not found in your hospital with ID: " + doctorId));

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        List<DoctorSchedule> activeSchedules = doctorScheduleRepository.findByDoctorIdAndHospitalIdAndDayOfWeekAndStatus(
                doctor.getId(), hospitalId, dayOfWeek, ScheduleStatus.ACTIVE
        );

        List<AvailableSlotDto> allSlots = new ArrayList<>();
        for (DoctorSchedule schedule : activeSchedules) {
            allSlots.addAll(slotGenerationService.generateSlots(schedule));
        }

        return DoctorAvailabilityResponse.builder()
                .doctorId(doctorId)
                .date(date.toString())
                .slots(allSlots)
                .build();
    }

    private void validateTimeAndBreak(
            LocalTime startTime,
            LocalTime endTime,
            Integer slotDurationMinutes,
            LocalTime breakStartTime,
            LocalTime breakEndTime) {

        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("startTime and endTime are required");
        }
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("startTime (" + startTime + ") must be before endTime (" + endTime + ")");
        }
        if (slotDurationMinutes == null || slotDurationMinutes <= 0) {
            throw new IllegalArgumentException("slotDurationMinutes must be positive");
        }

        boolean hasBreakStart = breakStartTime != null;
        boolean hasBreakEnd = breakEndTime != null;

        if (hasBreakStart != hasBreakEnd) {
            throw new IllegalArgumentException("Both breakStartTime and breakEndTime must be provided together");
        }

        if (hasBreakStart) {
            if (breakStartTime.isBefore(startTime)) {
                throw new IllegalArgumentException("breakStartTime cannot be before schedule startTime");
            }
            if (breakEndTime.isAfter(endTime)) {
                throw new IllegalArgumentException("breakEndTime cannot be after schedule endTime");
            }
            if (!breakStartTime.isBefore(breakEndTime)) {
                throw new IllegalArgumentException("breakStartTime must be before breakEndTime");
            }
        }
    }

    private void verifyAdminPermission(UserPrincipal currentUser) {
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_HOSPITAL_ADMIN") || a.getAuthority().equals("ROLE_PLATFORM_ADMIN"));
        if (!isAdmin) {
            throw new AccessDeniedException("Only Hospital Admins can manage doctor schedules");
        }
    }

    private void verifyAccessPermission(UserPrincipal currentUser) {
        boolean hasAccess = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_HOSPITAL_ADMIN") ||
                               a.getAuthority().equals("ROLE_PLATFORM_ADMIN") ||
                               a.getAuthority().equals("ROLE_DOCTOR"));
        if (!hasAccess) {
            throw new AccessDeniedException("You do not have permission to view doctor schedules");
        }
    }

    private void verifyViewPermission(UserPrincipal currentUser, DoctorSchedule schedule) {
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_HOSPITAL_ADMIN") || a.getAuthority().equals("ROLE_PLATFORM_ADMIN"));
        if (isAdmin) {
            return;
        }

        boolean isDoctor = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"));
        if (isDoctor && schedule.getDoctor().getUser().getId().equals(currentUser.getUserId())) {
            return;
        }

        throw new AccessDeniedException("You do not have permission to view this schedule");
    }
}
