package com.healthcare.backend.appointment.service;

import com.healthcare.backend.appointment.dto.AppointmentResponse;
import com.healthcare.backend.appointment.dto.CreateAppointmentRequest;
import com.healthcare.backend.appointment.dto.RescheduleAppointmentRequest;
import com.healthcare.backend.appointment.entity.Appointment;
import com.healthcare.backend.appointment.entity.AppointmentStatus;
import com.healthcare.backend.appointment.entity.AppointmentType;
import com.healthcare.backend.appointment.entity.BookingSource;
import com.healthcare.backend.appointment.repository.AppointmentRepository;
import com.healthcare.backend.auth.entity.RoleName;
import com.healthcare.backend.auth.entity.User;
import com.healthcare.backend.auth.repository.UserRepository;
import com.healthcare.backend.auth.security.SecurityUtils;
import com.healthcare.backend.auth.security.UserPrincipal;
import com.healthcare.backend.doctor.entity.Doctor;
import com.healthcare.backend.doctor.entity.DoctorStatus;
import com.healthcare.backend.doctor.repository.DoctorRepository;
import com.healthcare.backend.patient.entity.Patient;
import com.healthcare.backend.patient.entity.PatientStatus;
import com.healthcare.backend.patient.repository.PatientRepository;
import com.healthcare.backend.schedule.entity.DoctorSchedule;
import com.healthcare.backend.schedule.entity.ScheduleStatus;
import com.healthcare.backend.schedule.repository.DoctorScheduleRepository;
import com.healthcare.backend.notification.dto.CreateNotificationRequest;
import com.healthcare.backend.notification.entity.NotificationPriority;
import com.healthcare.backend.notification.entity.NotificationType;
import com.healthcare.backend.notification.service.NotificationService;
import com.healthcare.backend.teleconsult.dto.TeleconsultationMeeting;
import com.healthcare.backend.teleconsult.dto.TeleconsultationMeetingRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class AppointmentService {

    private static final Set<AppointmentStatus> ACTIVE_STATUSES = EnumSet.of(
            AppointmentStatus.BOOKED,
            AppointmentStatus.CONFIRMED,
            AppointmentStatus.CHECKED_IN,
            AppointmentStatus.IN_PROGRESS
    );

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final com.healthcare.backend.teleconsult.service.TeleconsultationService teleconsultationService;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            PatientRepository patientRepository,
            DoctorRepository doctorRepository,
            DoctorScheduleRepository doctorScheduleRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            com.healthcare.backend.teleconsult.service.TeleconsultationService teleconsultationService
    ) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.doctorScheduleRepository = doctorScheduleRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.teleconsultationService = teleconsultationService;
    }

    @Transactional
    public AppointmentResponse createAppointment(CreateAppointmentRequest request) {
        UserPrincipal currentUser = SecurityUtils.getRequiredCurrentUserPrincipal();
        Long hospitalId = SecurityUtils.getCurrentHospitalId();

        // 1. Role validation
        validateStaffBookingRole(currentUser);

        // 2. Patient validation
        Patient patient = patientRepository.findByIdAndHospitalId(request.getPatientId(), hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Patient not found with ID: " + request.getPatientId()));
        if (patient.getStatus() != PatientStatus.ACTIVE) {
            throw new IllegalArgumentException("Patient is not ACTIVE");
        }

        // 3. Doctor validation
        Doctor doctor = doctorRepository.findByIdAndHospitalId(request.getDoctorId(), hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Doctor not found with ID: " + request.getDoctorId()));
        if (doctor.getStatus() != DoctorStatus.ACTIVE) {
            throw new IllegalArgumentException("Doctor is not ACTIVE");
        }

        // 4. Date & Time past checks
        validateAppointmentDateTime(request.getAppointmentDate(), request.getStartTime());

        // 5. Schedule & Slot validation
        DoctorSchedule schedule = validateAndGetSchedule(doctor.getId(), hospitalId, request.getAppointmentDate(), request.getStartTime());
        LocalTime endTime = request.getStartTime().plusMinutes(schedule.getSlotDurationMinutes());

        // 6. Check existing active booking
        if (appointmentRepository.existsByDoctorIdAndAppointmentDateAndStartTimeAndStatusIn(
                doctor.getId(), request.getAppointmentDate(), request.getStartTime(), ACTIVE_STATUSES)) {
            throw new IllegalStateException("Doctor slot is already booked");
        }

        // 7. Determine appointment type & provision teleconsultation meeting if applicable
        AppointmentType appointmentType = request.getAppointmentType() != null ? request.getAppointmentType() : AppointmentType.NEW_CONSULTATION;
        String meetUrl = null;
        String teleconsultationSpaceId = null;

        if (appointmentType == AppointmentType.TELECONSULTATION) {
            String doctorName = (doctor.getUser() != null)
                    ? doctor.getUser().getFirstName() + " " + doctor.getUser().getLastName()
                    : "Doctor";
            String patientName = patient.getFirstName() + " " + patient.getLastName();

            try {
                com.healthcare.backend.teleconsult.dto.TeleconsultationMeeting meeting = teleconsultationService.createMeeting(
                        com.healthcare.backend.teleconsult.dto.TeleconsultationMeetingRequest.builder()
                                .hospitalId(hospitalId)
                                .doctorId(doctor.getId())
                                .doctorName(doctorName)
                                .patientId(patient.getId())
                                .patientName(patientName)
                                .appointmentDate(request.getAppointmentDate())
                                .startTime(request.getStartTime())
                                .durationMinutes(schedule.getSlotDurationMinutes())
                                .topic("Teleconsultation - " + doctorName)
                                .build()
                );
                meetUrl = meeting.getMeetUrl();
                teleconsultationSpaceId = meeting.getSpaceId();
            } catch (com.healthcare.backend.teleconsult.exception.TeleconsultationException te) {
                log.warn("Teleconsultation meeting could not be pre-provisioned at booking time: {}. Will be provisioned upon doctor session initiation.", te.getMessage());
            }
        }

        // Build Appointment
        Appointment appointment = new Appointment();
        appointment.setHospital(patient.getHospital());
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setAppointmentDate(request.getAppointmentDate());
        appointment.setStartTime(request.getStartTime());
        appointment.setEndTime(endTime);
        appointment.setAppointmentNumber(generateAppointmentNumber(request.getAppointmentDate()));
        appointment.setStatus(AppointmentStatus.BOOKED);
        appointment.setAppointmentType(appointmentType);
        appointment.setBookingSource(request.getBookingSource() != null ? request.getBookingSource() : BookingSource.RECEPTION);
        appointment.setReason(request.getReason());
        appointment.setNotes(request.getNotes());
        appointment.setMeetUrl(meetUrl);
        appointment.setTeleconsultationSpaceId(teleconsultationSpaceId);
        appointment.setActiveSlotKey(Appointment.buildActiveSlotKey(doctor.getId(), request.getAppointmentDate(), request.getStartTime()));

        try {
            Appointment saved = appointmentRepository.save(appointment);
            return mapToResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Doctor slot is already booked (concurrency conflict)", e);
        }
    }

    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentById(Long id) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        Appointment appointment = appointmentRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Appointment not found with ID: " + id));
        return mapToResponse(appointment);
    }

    @Transactional
    public AppointmentResponse startOrProvisionTeleconsultation(Long id) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        UserPrincipal currentUser = SecurityUtils.getRequiredCurrentUserPrincipal();

        Appointment appointment = appointmentRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Appointment not found with ID: " + id));

        // 1. Doctor authorization check: if doctor, must be assigned doctor
        boolean isStaffOrAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_HOSPITAL_ADMIN") ||
                               a.getAuthority().equals("ROLE_PLATFORM_ADMIN"));
        if (!isStaffOrAdmin) {
            Doctor doctor = doctorRepository.findByUserIdAndHospitalId(currentUser.getUserId(), hospitalId)
                    .orElseThrow(() -> new AccessDeniedException("Doctor profile not found for user: " + currentUser.getUserId()));
            if (!appointment.getDoctor().getId().equals(doctor.getId())) {
                throw new AccessDeniedException("Doctor can only start video consultations for their own appointments");
            }
        }

        // 2. Validate appointment type
        if (appointment.getAppointmentType() != AppointmentType.TELECONSULTATION) {
            throw new IllegalStateException("Appointment is an in-person consultation, not a teleconsultation");
        }

        // 3. Status eligibility check
        AppointmentStatus status = appointment.getStatus();
        if (status == AppointmentStatus.CANCELLED) {
            throw new IllegalStateException("Cannot start video consultation: Appointment is cancelled");
        }
        if (status == AppointmentStatus.COMPLETED) {
            throw new IllegalStateException("Cannot start video consultation: Appointment has already been completed");
        }
        if (status == AppointmentStatus.NO_SHOW) {
            throw new IllegalStateException("Cannot start video consultation: Appointment was marked as no-show");
        }

        // 4. Provision meeting if not yet generated
        if (appointment.getMeetUrl() == null || appointment.getMeetUrl().isBlank()) {
            String docName = appointment.getDoctor().getUser() != null
                    ? appointment.getDoctor().getUser().getFirstName() + " " + appointment.getDoctor().getUser().getLastName()
                    : "Doctor";
            String patName = appointment.getPatient() != null
                    ? appointment.getPatient().getFirstName() + " " + appointment.getPatient().getLastName()
                    : "Patient";

            long durationMinutes = (appointment.getStartTime() != null && appointment.getEndTime() != null)
                    ? Duration.between(appointment.getStartTime(), appointment.getEndTime()).toMinutes()
                    : 15L;

            TeleconsultationMeeting meeting = teleconsultationService.createMeeting(
                    TeleconsultationMeetingRequest.builder()
                            .appointmentId(appointment.getId())
                            .hospitalId(hospitalId)
                            .doctorId(appointment.getDoctor().getId())
                            .doctorName(docName)
                            .patientId(appointment.getPatient() != null ? appointment.getPatient().getId() : null)
                            .patientName(patName)
                            .appointmentDate(appointment.getAppointmentDate())
                            .startTime(appointment.getStartTime())
                            .durationMinutes((int) (durationMinutes > 0 ? durationMinutes : 15))
                            .topic("Teleconsultation - " + docName)
                            .build()
            );
            appointment.setMeetUrl(meeting.getMeetUrl());
            appointment.setTeleconsultationSpaceId(meeting.getSpaceId());
        }

        // 5. Update appointment status to IN_PROGRESS if eligible
        if (status == AppointmentStatus.BOOKED) {
            transitionStatus(appointment, AppointmentStatus.CONFIRMED);
            transitionStatus(appointment, AppointmentStatus.IN_PROGRESS);
        } else if (status == AppointmentStatus.CONFIRMED || status == AppointmentStatus.CHECKED_IN) {
            transitionStatus(appointment, AppointmentStatus.IN_PROGRESS);
        }

        Appointment saved = appointmentRepository.save(appointment);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponse> getAppointments(
            Long doctorId, Long patientId, LocalDate appointmentDate,
            AppointmentStatus status, AppointmentType appointmentType, String search, Pageable pageable
    ) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        String statusStr = status != null ? status.name() : null;
        String typeStr = appointmentType != null ? appointmentType.name() : null;
        return appointmentRepository.findByHospitalIdWithFilters(
                hospitalId, doctorId, patientId, appointmentDate, statusStr, typeStr, search, pageable
        ).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponse> getTodayAppointments(Long doctorId, Pageable pageable) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        LocalDate today = LocalDate.now();
        return appointmentRepository.findByHospitalIdWithFilters(
                hospitalId, doctorId, null, today, null, null, null, pageable
        ).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponse> getDoctorAppointments(
            Long doctorId, LocalDate date, AppointmentStatus status, AppointmentType appointmentType, String search, Pageable pageable
    ) {
        UserPrincipal currentUser = SecurityUtils.getRequiredCurrentUserPrincipal();
        Long hospitalId = SecurityUtils.getCurrentHospitalId();

        boolean isStaffOrAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_HOSPITAL_ADMIN") ||
                               a.getAuthority().equals("ROLE_PLATFORM_ADMIN") ||
                               a.getAuthority().equals("ROLE_RECEPTIONIST"));
        if (!isStaffOrAdmin) {
            Doctor targetDoctor = doctorRepository.findByIdAndHospitalId(doctorId, hospitalId)
                    .orElseThrow(() -> new NoSuchElementException("Doctor not found with ID: " + doctorId));
            if (!targetDoctor.getUser().getId().equals(currentUser.getUserId())) {
                throw new AccessDeniedException("Doctor can only view their own appointments");
            }
        }

        String statusStr = status != null ? status.name() : null;
        String typeStr = appointmentType != null ? appointmentType.name() : null;
        return appointmentRepository.findByHospitalIdWithFilters(
                hospitalId, doctorId, null, date, statusStr, typeStr, search, pageable
        ).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponse> getPatientAppointments(Long patientId, Pageable pageable) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        Patient patient = patientRepository.findByIdAndHospitalId(patientId, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Patient not found with ID: " + patientId));
        return appointmentRepository.findByHospitalIdWithFilters(
                hospitalId, null, patient.getId(), null, null, null, null, pageable
        ).map(this::mapToResponse);
    }

    @Transactional
    public AppointmentResponse confirmAppointment(Long id) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        Appointment appointment = appointmentRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Appointment not found with ID: " + id));

        // Provision meeting idempotently if teleconsultation appointment lacks meeting details
        if (appointment.getAppointmentType() == AppointmentType.TELECONSULTATION) {
            if (appointment.getMeetUrl() == null || appointment.getMeetUrl().isBlank()) {
                DoctorSchedule schedule = validateAndGetSchedule(
                        appointment.getDoctor().getId(), hospitalId, appointment.getAppointmentDate(), appointment.getStartTime());
                String docName = appointment.getDoctor().getUser() != null
                        ? appointment.getDoctor().getUser().getFirstName() + " " + appointment.getDoctor().getUser().getLastName()
                        : "Doctor";
                String patName = appointment.getPatient() != null
                        ? appointment.getPatient().getFirstName() + " " + appointment.getPatient().getLastName()
                        : "Patient";

                com.healthcare.backend.teleconsult.dto.TeleconsultationMeeting meeting = teleconsultationService.createMeeting(
                        com.healthcare.backend.teleconsult.dto.TeleconsultationMeetingRequest.builder()
                                .appointmentId(appointment.getId())
                                .hospitalId(hospitalId)
                                .doctorId(appointment.getDoctor().getId())
                                .doctorName(docName)
                                .patientId(appointment.getPatient() != null ? appointment.getPatient().getId() : null)
                                .patientName(patName)
                                .appointmentDate(appointment.getAppointmentDate())
                                .startTime(appointment.getStartTime())
                                .durationMinutes(schedule.getSlotDurationMinutes())
                                .topic("Teleconsultation - " + docName)
                                .build()
                );
                appointment.setMeetUrl(meeting.getMeetUrl());
                appointment.setTeleconsultationSpaceId(meeting.getSpaceId());
            }
        }

        transitionStatus(appointment, AppointmentStatus.CONFIRMED);
        Appointment saved = appointmentRepository.save(appointment);

        String patientName = saved.getPatient() != null ? saved.getPatient().getFirstName() + " " + saved.getPatient().getLastName() : "Patient";
        String msg = String.format("Appointment for %s has been confirmed for %s at %s.",
                patientName, saved.getAppointmentDate(), saved.getStartTime());
        createAppointmentNotification(saved, NotificationType.APPOINTMENT_CONFIRMED, "Appointment Confirmed", msg);

        return mapToResponse(saved);
    }

    @Transactional
    public AppointmentResponse checkInAppointment(Long id) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        Appointment appointment = appointmentRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Appointment not found with ID: " + id));

        transitionStatus(appointment, AppointmentStatus.CHECKED_IN);
        return mapToResponse(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentResponse markNoShowAppointment(Long id) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        Appointment appointment = appointmentRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Appointment not found with ID: " + id));

        transitionStatus(appointment, AppointmentStatus.NO_SHOW);
        appointment.setActiveSlotKey(null); // Release slot for future booking

        return mapToResponse(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentResponse cancelAppointment(Long id, String reason) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        Appointment appointment = appointmentRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Appointment not found with ID: " + id));

        transitionStatus(appointment, AppointmentStatus.CANCELLED);
        appointment.setCancellationReason(reason);
        appointment.setCancelledAt(LocalDateTime.now());
        appointment.setActiveSlotKey(null); // Release slot for future booking

        Appointment saved = appointmentRepository.save(appointment);

        String patientName = saved.getPatient() != null ? saved.getPatient().getFirstName() + " " + saved.getPatient().getLastName() : "Patient";
        String reasonStr = (reason != null && !reason.isBlank()) ? " Reason: " + reason : "";
        String msg = String.format("Appointment for %s on %s at %s has been cancelled.%s",
                patientName, saved.getAppointmentDate(), saved.getStartTime(), reasonStr);
        createAppointmentNotification(saved, NotificationType.APPOINTMENT_CANCELLED, "Appointment Cancelled", msg);

        return mapToResponse(saved);
    }

    @Transactional
    public AppointmentResponse rescheduleAppointment(Long id, RescheduleAppointmentRequest request) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        Appointment appointment = appointmentRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Appointment not found with ID: " + id));

        if (appointment.getStatus() == AppointmentStatus.COMPLETED ||
            appointment.getStatus() == AppointmentStatus.CANCELLED ||
            appointment.getStatus() == AppointmentStatus.NO_SHOW) {
            throw new IllegalArgumentException("Cannot reschedule appointment with status: " + appointment.getStatus());
        }

        // Validate new Date & Time
        validateAppointmentDateTime(request.getAppointmentDate(), request.getStartTime());

        // Validate Doctor Schedule for new time
        DoctorSchedule schedule = validateAndGetSchedule(appointment.getDoctor().getId(), hospitalId, request.getAppointmentDate(), request.getStartTime());
        LocalTime newEndTime = request.getStartTime().plusMinutes(schedule.getSlotDurationMinutes());

        // Check if slot is occupied (excluding current appointment)
        if (appointmentRepository.existsByDoctorIdAndAppointmentDateAndStartTimeAndStatusIn(
                appointment.getDoctor().getId(), request.getAppointmentDate(), request.getStartTime(), ACTIVE_STATUSES)) {
            if (!(appointment.getAppointmentDate().equals(request.getAppointmentDate()) && appointment.getStartTime().equals(request.getStartTime()))) {
                throw new IllegalStateException("New doctor slot is already booked");
            }
        }

        appointment.setAppointmentDate(request.getAppointmentDate());
        appointment.setStartTime(request.getStartTime());
        appointment.setEndTime(newEndTime);
        appointment.setActiveSlotKey(Appointment.buildActiveSlotKey(appointment.getDoctor().getId(), request.getAppointmentDate(), request.getStartTime()));

        try {
            Appointment saved = appointmentRepository.save(appointment);

            String patientName = saved.getPatient() != null ? saved.getPatient().getFirstName() + " " + saved.getPatient().getLastName() : "Patient";
            String msg = String.format("Appointment for %s has been rescheduled to %s at %s.",
                    patientName, saved.getAppointmentDate(), saved.getStartTime());
            createAppointmentNotification(saved, NotificationType.APPOINTMENT_RESCHEDULED, "Appointment Rescheduled", msg);

            return mapToResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("New doctor slot is already booked (concurrency conflict)", e);
        }
    }

    private void createAppointmentNotification(Appointment appointment, NotificationType type, String title, String message) {
        if (notificationService == null || appointment == null || appointment.getDoctor() == null || appointment.getDoctor().getUser() == null) {
            return;
        }
        try {
            CreateNotificationRequest req = new CreateNotificationRequest();
            req.setHospitalId(appointment.getHospital().getId());
            req.setRecipientUserId(appointment.getDoctor().getUser().getId());
            req.setType(type);
            req.setTitle(title);
            req.setMessage(message);
            req.setPriority(NotificationPriority.NORMAL);
            req.setReferenceType("APPOINTMENT");
            req.setReferenceId(appointment.getId());

            notificationService.createNotification(req);
        } catch (Exception e) {
            // Safe fallback so core appointment operation is preserved
        }
    }

    private void validateStaffBookingRole(UserPrincipal currentUser) {
        boolean isAllowed = currentUser.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_HOSPITAL_ADMIN") ||
                a.getAuthority().equals("ROLE_RECEPTIONIST") ||
                a.getAuthority().equals("ROLE_DOCTOR") ||
                a.getAuthority().equals("ROLE_PLATFORM_ADMIN")
        );

        if (!isAllowed) {
            throw new AccessDeniedException("User role does not have permission to book appointments");
        }
    }

    private void validateAppointmentDateTime(LocalDate appointmentDate, LocalTime startTime) {
        LocalDate today = LocalDate.now();
        if (appointmentDate.isBefore(today)) {
            throw new IllegalArgumentException("Cannot book appointment in the past");
        }
        if (appointmentDate.isEqual(today) && startTime.isBefore(LocalTime.now())) {
            throw new IllegalArgumentException("Start time must be in the future");
        }
    }

    private DoctorSchedule validateAndGetSchedule(Long doctorId, Long hospitalId, LocalDate date, LocalTime startTime) {
        List<DoctorSchedule> schedules = doctorScheduleRepository.findByDoctorIdAndHospitalIdAndDayOfWeekAndStatus(
                doctorId, hospitalId, date.getDayOfWeek(), ScheduleStatus.ACTIVE
        );

        if (schedules.isEmpty()) {
            throw new IllegalArgumentException("Doctor has no active schedule for " + date.getDayOfWeek());
        }

        DoctorSchedule matchingSchedule = schedules.stream()
                .filter(s -> !startTime.isBefore(s.getStartTime()) && startTime.isBefore(s.getEndTime()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Requested start time is outside doctor's work hours"));

        if (matchingSchedule.getBreakStartTime() != null && matchingSchedule.getBreakEndTime() != null) {
            LocalTime bStart = matchingSchedule.getBreakStartTime();
            LocalTime bEnd = matchingSchedule.getBreakEndTime();
            if (startTime.isAfter(bStart.minusNanos(1)) && startTime.isBefore(bEnd)) {
                throw new IllegalArgumentException("Requested start time falls inside doctor's break interval");
            }
        }

        long minutesFromStart = Duration.between(matchingSchedule.getStartTime(), startTime).toMinutes();
        if (minutesFromStart % matchingSchedule.getSlotDurationMinutes() != 0) {
            throw new IllegalArgumentException("Requested start time does not align with slot duration grid (" + matchingSchedule.getSlotDurationMinutes() + " mins)");
        }

        LocalTime endTime = startTime.plusMinutes(matchingSchedule.getSlotDurationMinutes());
        if (endTime.isAfter(matchingSchedule.getEndTime())) {
            throw new IllegalArgumentException("Appointment extends beyond schedule end time");
        }

        if (matchingSchedule.getBreakStartTime() != null && matchingSchedule.getBreakEndTime() != null) {
            LocalTime bStart = matchingSchedule.getBreakStartTime();
            LocalTime bEnd = matchingSchedule.getBreakEndTime();
            if (startTime.isBefore(bEnd) && endTime.isAfter(bStart)) {
                throw new IllegalArgumentException("Appointment slot overlaps with break interval");
            }
        }

        return matchingSchedule;
    }

    private void transitionStatus(Appointment appointment, AppointmentStatus targetStatus) {
        AppointmentStatus current = appointment.getStatus();

        if (current == AppointmentStatus.COMPLETED || current == AppointmentStatus.CANCELLED || current == AppointmentStatus.NO_SHOW) {
            throw new IllegalArgumentException("Cannot transition from terminal status " + current + " to " + targetStatus);
        }

        if (targetStatus == AppointmentStatus.CONFIRMED) {
            if (current != AppointmentStatus.BOOKED) {
                throw new IllegalArgumentException("Cannot confirm appointment with status: " + current);
            }
        } else if (targetStatus == AppointmentStatus.CHECKED_IN) {
            if (current != AppointmentStatus.BOOKED && current != AppointmentStatus.CONFIRMED) {
                throw new IllegalArgumentException("Cannot check-in appointment with status: " + current);
            }
        } else if (targetStatus == AppointmentStatus.IN_PROGRESS) {
            if (current != AppointmentStatus.CONFIRMED && current != AppointmentStatus.CHECKED_IN) {
                throw new IllegalArgumentException("Cannot start consultation for appointment with status: " + current);
            }
        } else if (targetStatus == AppointmentStatus.COMPLETED) {
            if (current != AppointmentStatus.IN_PROGRESS && current != AppointmentStatus.CHECKED_IN && current != AppointmentStatus.CONFIRMED) {
                throw new IllegalArgumentException("Cannot complete appointment with status: " + current);
            }
        } else if (targetStatus == AppointmentStatus.NO_SHOW) {
            if (current != AppointmentStatus.BOOKED && current != AppointmentStatus.CONFIRMED && current != AppointmentStatus.CHECKED_IN) {
                throw new IllegalArgumentException("Cannot mark no-show for appointment with status: " + current);
            }
        }

        appointment.setStatus(targetStatus);
    }

    private String generateAppointmentNumber(LocalDate date) {
        String dateStr = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "APT-" + dateStr + "-" + randomSuffix;
    }

    private AppointmentResponse mapToResponse(Appointment appointment) {
        AppointmentResponse res = new AppointmentResponse();
        res.setId(appointment.getId());
        res.setAppointmentNumber(appointment.getAppointmentNumber());
        res.setHospitalId(appointment.getHospital().getId());
        res.setPatientId(appointment.getPatient().getId());
        res.setPatientName(appointment.getPatient().getFirstName() + " " + appointment.getPatient().getLastName());
        res.setDoctorId(appointment.getDoctor().getId());
        res.setDoctorName(appointment.getDoctor().getUser().getFirstName() + " " + appointment.getDoctor().getUser().getLastName());
        res.setSpecialization(appointment.getDoctor().getSpecialization());
        res.setAppointmentDate(appointment.getAppointmentDate());
        res.setStartTime(appointment.getStartTime());
        res.setEndTime(appointment.getEndTime());
        res.setStatus(appointment.getStatus());
        res.setAppointmentType(appointment.getAppointmentType());
        res.setBookingSource(appointment.getBookingSource());
        res.setReason(appointment.getReason());
        res.setNotes(appointment.getNotes());
        res.setCancellationReason(appointment.getCancellationReason());
        res.setCancelledAt(appointment.getCancelledAt());
        res.setCreatedAt(appointment.getCreatedAt());
        res.setUpdatedAt(appointment.getUpdatedAt());
        res.setMeetUrl(appointment.getMeetUrl());
        res.setTeleconsultationSpaceId(appointment.getTeleconsultationSpaceId());
        res.setIsTeleconsultation(appointment.isTeleconsultation());
        return res;
    }
}
