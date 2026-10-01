package com.healthcare.backend.queue.service;

import com.healthcare.backend.appointment.entity.Appointment;
import com.healthcare.backend.appointment.entity.AppointmentStatus;
import com.healthcare.backend.appointment.repository.AppointmentRepository;
import com.healthcare.backend.auth.security.SecurityUtils;

import com.healthcare.backend.doctor.entity.Doctor;
import com.healthcare.backend.doctor.entity.DoctorStatus;
import com.healthcare.backend.doctor.repository.DoctorRepository;
import com.healthcare.backend.patient.entity.Patient;
import com.healthcare.backend.patient.repository.PatientRepository;
import com.healthcare.backend.queue.dto.*;
import com.healthcare.backend.queue.entity.*;
import com.healthcare.backend.queue.repository.QueueEntryRepository;
import com.healthcare.backend.notification.dto.CreateNotificationRequest;
import com.healthcare.backend.notification.entity.NotificationPriority;
import com.healthcare.backend.notification.entity.NotificationType;
import com.healthcare.backend.notification.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class QueueService {

    private final QueueEntryRepository queueEntryRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final EstimatedWaitTimeService estimatedWaitTimeService;
    private final NotificationService notificationService;

    public QueueService(
            QueueEntryRepository queueEntryRepository,
            DoctorRepository doctorRepository,
            PatientRepository patientRepository,
            AppointmentRepository appointmentRepository,
            EstimatedWaitTimeService estimatedWaitTimeService,
            NotificationService notificationService
    ) {
        this.queueEntryRepository = queueEntryRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.estimatedWaitTimeService = estimatedWaitTimeService;
        this.notificationService = notificationService;
    }

    @Transactional
    public QueueEntryResponse createQueueEntry(CreateQueueEntryRequest request) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        LocalDate today = LocalDate.now();

        Doctor doctor = doctorRepository.findByIdAndHospitalId(request.getDoctorId(), hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Doctor not found in your hospital: " + request.getDoctorId()));

        if (doctor.getStatus() != DoctorStatus.ACTIVE) {
            throw new IllegalStateException("Doctor is not active: " + doctor.getId());
        }

        Patient patient = patientRepository.findByIdAndHospitalId(request.getPatientId(), hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Patient not found in your hospital: " + request.getPatientId()));

        Appointment appointment = null;
        if (request.getAppointmentId() != null) {
            appointment = appointmentRepository.findByIdAndHospitalId(request.getAppointmentId(), hospitalId)
                    .orElseThrow(() -> new NoSuchElementException("Appointment not found in your hospital: " + request.getAppointmentId()));

            if (!appointment.getDoctor().getId().equals(doctor.getId())) {
                throw new IllegalArgumentException("Appointment doctor does not match selected doctor");
            }
            if (!appointment.getPatient().getId().equals(patient.getId())) {
                throw new IllegalArgumentException("Appointment patient does not match selected patient");
            }
            if (!appointment.getAppointmentDate().equals(today)) {
                throw new IllegalArgumentException("Appointment date (" + appointment.getAppointmentDate() + ") does not match queue date (" + today + ")");
            }

            Optional<QueueEntry> existingApptQueue = queueEntryRepository.findActiveEntryByAppointment(hospitalId, appointment.getId());
            if (existingApptQueue.isPresent()) {
                throw new IllegalStateException("Appointment is already checked into an active queue");
            }

            if (appointment.getStatus() == AppointmentStatus.CHECKED_IN) {
                throw new IllegalStateException("Appointment is already checked in");
            }

            appointment.setStatus(AppointmentStatus.CHECKED_IN);
            appointmentRepository.save(appointment);
        }

        Optional<QueueEntry> activePatientQueue = queueEntryRepository.findActiveEntryByPatientAndDoctorAndDate(
                hospitalId, doctor.getId(), patient.getId(), today
        );
        if (activePatientQueue.isPresent()) {
            throw new IllegalStateException("Patient is already actively queued for this doctor today");
        }

        Integer nextToken = queueEntryRepository.findMaxTokenNumber(hospitalId, doctor.getId(), today) + 1;

        QueueEntry entry = new QueueEntry();
        entry.setHospital(doctor.getHospital());
        entry.setDoctor(doctor);
        entry.setPatient(patient);
        entry.setAppointment(appointment);
        entry.setQueueDate(today);
        entry.setTokenNumber(nextToken);
        entry.setPriority(request.getPriority() != null ? request.getPriority() : QueuePriority.NORMAL);
        entry.setStatus(QueueStatus.WAITING);
        entry.setCheckedInAt(LocalDateTime.now());
        entry.setNotes(request.getNotes());
        entry.setActiveQueueKey(QueueEntry.buildActiveQueueKey(hospitalId, doctor.getId(), patient.getId(), today));

        int priorityVal = getPriorityValue(entry.getPriority());
        int estimatedWait = estimatedWaitTimeService.calculateEstimatedWaitMinutes(
                hospitalId, doctor.getId(), today, priorityVal, entry.getCheckedInAt()
        );
        entry.setEstimatedWaitMinutes(estimatedWait);

        QueueEntry saved = queueEntryRepository.save(entry);

        String patientName = saved.getPatient() != null ? saved.getPatient().getFirstName() + " " + saved.getPatient().getLastName() : "Patient";
        String msg = String.format("%s (Token #%d) has checked in and is waiting in your queue.",
                patientName, saved.getTokenNumber());
        createQueueNotification(saved, NotificationType.PATIENT_CHECKED_IN, "Patient Checked In", msg);

        return QueueEntryResponse.fromEntity(saved);
    }

    @Transactional
    public QueueEntryResponse checkInAppointment(Long appointmentId) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();

        Appointment appointment = appointmentRepository.findByIdAndHospitalId(appointmentId, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Appointment not found in your hospital: " + appointmentId));

        CreateQueueEntryRequest request = new CreateQueueEntryRequest();
        request.setDoctorId(appointment.getDoctor().getId());
        request.setPatientId(appointment.getPatient().getId());
        request.setAppointmentId(appointment.getId());
        request.setPriority(QueuePriority.NORMAL);
        request.setNotes(appointment.getReason());

        return createQueueEntry(request);
    }

    @Transactional(readOnly = true)
    public Page<QueueEntryResponse> getQueueEntries(Long doctorId, LocalDate date, String statusStr, String priorityStr, Pageable pageable) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        LocalDate queryDate = date != null ? date : LocalDate.now();

        QueueStatus status = null;
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                status = QueueStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        QueuePriority priority = null;
        if (priorityStr != null && !priorityStr.isBlank()) {
            try {
                priority = QueuePriority.valueOf(priorityStr.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        Page<QueueEntry> entries = queueEntryRepository.findByHospitalIdWithFilters(
                hospitalId, doctorId, queryDate, status, priority, pageable
        );

        return entries.map(entry -> {
            if (entry.getStatus() == QueueStatus.WAITING) {
                int priorityVal = getPriorityValue(entry.getPriority());
                int estWait = estimatedWaitTimeService.calculateEstimatedWaitMinutes(
                        hospitalId, entry.getDoctor().getId(), entry.getQueueDate(), priorityVal, entry.getCheckedInAt()
                );
                entry.setEstimatedWaitMinutes(estWait);
            }
            return QueueEntryResponse.fromEntity(entry);
        });
    }


    @Transactional(readOnly = true)
    public Page<QueueEntryResponse> getDoctorOwnQueue(LocalDate date, Pageable pageable) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        Long userId = SecurityUtils.getRequiredCurrentUserPrincipal().getUserId();

        Doctor doctor = doctorRepository.findByUserIdAndHospitalId(userId, hospitalId)
                .orElseThrow(() -> new AccessDeniedException("Only doctors can access their own queue"));

        return getQueueEntries(doctor.getId(), date != null ? date : LocalDate.now(), null, null, pageable);
    }

    @Transactional(readOnly = true)
    public QueueEntryResponse getQueueEntryById(Long id) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        QueueEntry entry = queueEntryRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Queue entry not found: " + id));
        return QueueEntryResponse.fromEntity(entry);
    }

    @Transactional(readOnly = true)
    public CurrentTokenResponse getCurrentConsultation(Long doctorId) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        LocalDate today = LocalDate.now();

        Optional<QueueEntry> current = queueEntryRepository.findCurrentConsultationForDoctor(hospitalId, doctorId, today);
        return current.map(CurrentTokenResponse::fromEntity).orElse(null);
    }

    @Transactional(readOnly = true)
    public CurrentTokenResponse getDoctorOwnCurrentConsultation() {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        Long userId = SecurityUtils.getRequiredCurrentUserPrincipal().getUserId();

        Doctor doctor = doctorRepository.findByUserIdAndHospitalId(userId, hospitalId)
                .orElseThrow(() -> new AccessDeniedException("Only doctors can access their current token"));

        return getCurrentConsultation(doctor.getId());
    }

    @Transactional
    public QueueEntryResponse callPatient(Long id) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        QueueEntry entry = queueEntryRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Queue entry not found: " + id));

        validateDoctorOwnershipIfDoctorRole(entry.getDoctor());

        if (entry.getStatus() != QueueStatus.WAITING) {
            throw new IllegalStateException("Cannot call patient with status: " + entry.getStatus() + ". Only WAITING patients can be CALLED.");
        }

        entry.setStatus(QueueStatus.CALLED);
        entry.setCalledAt(LocalDateTime.now());

        QueueEntry updated = queueEntryRepository.save(entry);

        String patientName = updated.getPatient() != null ? updated.getPatient().getFirstName() + " " + updated.getPatient().getLastName() : "Patient";
        String msg = String.format("Patient %s (Token #%d) has been called.",
                patientName, updated.getTokenNumber());
        createQueueNotification(updated, NotificationType.QUEUE_UPDATE, "Queue Update", msg);

        return QueueEntryResponse.fromEntity(updated);
    }

    @Transactional
    public QueueEntryResponse startConsultation(Long id) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        QueueEntry entry = queueEntryRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Queue entry not found: " + id));

        validateDoctorOwnershipIfDoctorRole(entry.getDoctor());

        if (entry.getStatus() != QueueStatus.CALLED && entry.getStatus() != QueueStatus.WAITING) {
            throw new IllegalStateException("Cannot start consultation for entry with status: " + entry.getStatus());
        }

        Optional<QueueEntry> activeConsultation = queueEntryRepository.findCurrentConsultationForDoctor(
                hospitalId, entry.getDoctor().getId(), entry.getQueueDate()
        );
        if (activeConsultation.isPresent() && !activeConsultation.get().getId().equals(entry.getId())) {
            throw new IllegalStateException("Doctor already has a patient in consultation (Token " + activeConsultation.get().getTokenNumber() + ")");
        }

        entry.setStatus(QueueStatus.IN_CONSULTATION);
        entry.setConsultationStartedAt(LocalDateTime.now());

        QueueEntry updated = queueEntryRepository.save(entry);
        return QueueEntryResponse.fromEntity(updated);
    }

    @Transactional
    public QueueEntryResponse completeConsultation(Long id) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        QueueEntry entry = queueEntryRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Queue entry not found: " + id));

        validateDoctorOwnershipIfDoctorRole(entry.getDoctor());

        if (entry.getStatus() != QueueStatus.IN_CONSULTATION) {
            throw new IllegalStateException("Cannot complete consultation for entry with status: " + entry.getStatus() + ". Must be IN_CONSULTATION.");
        }

        LocalDateTime now = LocalDateTime.now();
        entry.setStatus(QueueStatus.COMPLETED);
        entry.setCompletedAt(now);
        entry.setActiveQueueKey(null);

        if (entry.getCheckedInAt() != null && entry.getConsultationStartedAt() != null) {
            long waitMinutes = Duration.between(entry.getCheckedInAt(), entry.getConsultationStartedAt()).toMinutes();
            entry.setActualWaitMinutes((int) Math.max(0, waitMinutes));
        }

        if (entry.getConsultationStartedAt() != null) {
            long durationMinutes = Duration.between(entry.getConsultationStartedAt(), now).toMinutes();
            entry.setConsultationDurationMinutes((int) Math.max(1, durationMinutes));
        }

        if (entry.getAppointment() != null) {
            entry.getAppointment().setStatus(AppointmentStatus.COMPLETED);
            appointmentRepository.save(entry.getAppointment());
        }

        QueueEntry updated = queueEntryRepository.save(entry);
        return QueueEntryResponse.fromEntity(updated);
    }

    @Transactional
    public QueueEntryResponse skipPatient(Long id, String reason) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        QueueEntry entry = queueEntryRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Queue entry not found: " + id));

        validateDoctorOwnershipIfDoctorRole(entry.getDoctor());

        if (entry.getStatus() != QueueStatus.WAITING && entry.getStatus() != QueueStatus.CALLED) {
            throw new IllegalStateException("Cannot skip queue entry with status: " + entry.getStatus());
        }

        entry.setStatus(QueueStatus.SKIPPED);
        entry.setSkipReason(reason);
        entry.setActiveQueueKey(null);

        QueueEntry updated = queueEntryRepository.save(entry);

        String patientName = updated.getPatient() != null ? updated.getPatient().getFirstName() + " " + updated.getPatient().getLastName() : "Patient";
        String reasonStr = (reason != null && !reason.isBlank()) ? " Reason: " + reason : "";
        String msg = String.format("Patient %s (Token #%d) was skipped in the queue.%s",
                patientName, updated.getTokenNumber(), reasonStr);
        createQueueNotification(updated, NotificationType.QUEUE_UPDATE, "Queue Update", msg);

        return QueueEntryResponse.fromEntity(updated);
    }

    @Transactional
    public QueueEntryResponse recallPatient(Long id) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        QueueEntry entry = queueEntryRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Queue entry not found: " + id));

        validateDoctorOwnershipIfDoctorRole(entry.getDoctor());

        if (entry.getStatus() != QueueStatus.SKIPPED) {
            throw new IllegalStateException("Cannot recall patient with status: " + entry.getStatus() + ". Only SKIPPED patients can be recalled.");
        }

        Optional<QueueEntry> activePatientQueue = queueEntryRepository.findActiveEntryByPatientAndDoctorAndDate(
                hospitalId, entry.getDoctor().getId(), entry.getPatient().getId(), entry.getQueueDate()
        );
        if (activePatientQueue.isPresent()) {
            throw new IllegalStateException("Patient is already actively queued for this doctor today");
        }

        entry.setStatus(QueueStatus.WAITING);
        entry.setSkipReason(null);
        entry.setActiveQueueKey(QueueEntry.buildActiveQueueKey(hospitalId, entry.getDoctor().getId(), entry.getPatient().getId(), entry.getQueueDate()));

        QueueEntry updated = queueEntryRepository.save(entry);

        String patientName = updated.getPatient() != null ? updated.getPatient().getFirstName() + " " + updated.getPatient().getLastName() : "Patient";
        String msg = String.format("%s (Token #%d) is waiting in your queue.",
                patientName, updated.getTokenNumber());
        createQueueNotification(updated, NotificationType.PATIENT_WAITING, "Patient Waiting", msg);

        return QueueEntryResponse.fromEntity(updated);
    }

    private void createQueueNotification(QueueEntry entry, NotificationType type, String title, String message) {
        if (notificationService == null || entry == null || entry.getDoctor() == null || entry.getDoctor().getUser() == null) {
            return;
        }
        try {
            CreateNotificationRequest req = new CreateNotificationRequest();
            req.setHospitalId(entry.getHospital().getId());
            req.setRecipientUserId(entry.getDoctor().getUser().getId());
            req.setType(type);
            req.setTitle(title);
            req.setMessage(message);
            req.setPriority(NotificationPriority.NORMAL);
            req.setReferenceType("QUEUE");
            req.setReferenceId(entry.getId());

            notificationService.createNotification(req);
        } catch (Exception e) {
            // Safe fallback so core queue operation is preserved
        }
    }

    @Transactional
    public QueueEntryResponse cancelQueueEntry(Long id, String reason) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        QueueEntry entry = queueEntryRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Queue entry not found: " + id));

        validateDoctorOwnershipIfDoctorRole(entry.getDoctor());

        if (entry.getStatus() == QueueStatus.COMPLETED || entry.getStatus() == QueueStatus.CANCELLED) {
            throw new IllegalStateException("Cannot cancel queue entry with status: " + entry.getStatus());
        }

        entry.setStatus(QueueStatus.CANCELLED);
        entry.setCancellationReason(reason);
        entry.setActiveQueueKey(null);

        if (entry.getAppointment() != null) {
            entry.getAppointment().setStatus(AppointmentStatus.CANCELLED);
            entry.getAppointment().setCancellationReason("Queue entry cancelled: " + reason);
            entry.getAppointment().setCancelledAt(LocalDateTime.now());
            entry.getAppointment().setActiveSlotKey(null);
            appointmentRepository.save(entry.getAppointment());
        }

        QueueEntry updated = queueEntryRepository.save(entry);
        return QueueEntryResponse.fromEntity(updated);
    }

    @Transactional
    public QueueEntryResponse callNextPatientForDoctor() {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        Long userId = SecurityUtils.getRequiredCurrentUserPrincipal().getUserId();

        Doctor doctor = doctorRepository.findByUserIdAndHospitalId(userId, hospitalId)
                .orElseThrow(() -> new AccessDeniedException("Only doctors can call their next patient"));

        LocalDate today = LocalDate.now();

        Optional<QueueEntry> activeConsultation = queueEntryRepository.findCurrentConsultationForDoctor(hospitalId, doctor.getId(), today);
        if (activeConsultation.isPresent()) {
            throw new IllegalStateException("Doctor already has a patient in consultation (Token " + activeConsultation.get().getTokenNumber() + ")");
        }

        List<QueueEntry> nextEntries = queueEntryRepository.findNextWaitingEntriesForDoctor(hospitalId, doctor.getId(), today, PageRequest.of(0, 1));
        if (nextEntries.isEmpty()) {
            throw new NoSuchElementException("No waiting patients in queue for today");
        }

        QueueEntry next = nextEntries.get(0);
        next.setStatus(QueueStatus.IN_CONSULTATION);
        next.setConsultationStartedAt(LocalDateTime.now());

        QueueEntry updated = queueEntryRepository.save(next);
        return QueueEntryResponse.fromEntity(updated);
    }

    @Transactional
    public DoctorQueueStatusResponse updateDoctorOwnQueueStatus(DoctorQueueStatus status) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        Long userId = SecurityUtils.getRequiredCurrentUserPrincipal().getUserId();

        Doctor doctor = doctorRepository.findByUserIdAndHospitalId(userId, hospitalId)
                .orElseThrow(() -> new AccessDeniedException("Only doctors can update their arrival status"));

        doctor.setQueueStatus(status);
        Doctor saved = doctorRepository.save(doctor);
        return DoctorQueueStatusResponse.fromDoctor(saved);
    }

    @Transactional(readOnly = true)
    public DoctorQueueStatusResponse getDoctorQueueStatus(Long doctorId) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        Doctor doctor = doctorRepository.findByIdAndHospitalId(doctorId, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Doctor not found in your hospital: " + doctorId));
        return DoctorQueueStatusResponse.fromDoctor(doctor);
    }

    private int getPriorityValue(QueuePriority priority) {
        if (priority == null) return 1;
        return switch (priority) {
            case EMERGENCY -> 3;
            case URGENT -> 2;
            case NORMAL -> 1;
        };
    }

    private void validateDoctorOwnershipIfDoctorRole(Doctor doctor) {
        var userPrincipal = SecurityUtils.getRequiredCurrentUserPrincipal();
        boolean isDoctorRole = userPrincipal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"));

        if (isDoctorRole) {
            Doctor loggedInDoctor = doctorRepository.findByUserIdAndHospitalId(userPrincipal.getUserId(), userPrincipal.getHospitalId())
                    .orElse(null);

            if (loggedInDoctor != null && !loggedInDoctor.getId().equals(doctor.getId())) {
                throw new AccessDeniedException("Doctors can only manage their own queue");
            }
        }
    }
}
