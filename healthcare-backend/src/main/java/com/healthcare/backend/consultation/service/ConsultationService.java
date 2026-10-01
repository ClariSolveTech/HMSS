package com.healthcare.backend.consultation.service;

import com.healthcare.backend.appointment.entity.Appointment;
import com.healthcare.backend.appointment.repository.AppointmentRepository;
import com.healthcare.backend.auth.security.SecurityUtils;

import com.healthcare.backend.consultation.dto.ConsultationResponse;
import com.healthcare.backend.consultation.dto.CreateConsultationRequest;
import com.healthcare.backend.consultation.dto.UpdateConsultationRequest;
import com.healthcare.backend.consultation.entity.Consultation;
import com.healthcare.backend.consultation.entity.ConsultationStatus;
import com.healthcare.backend.consultation.repository.ConsultationRepository;
import com.healthcare.backend.doctor.entity.Doctor;
import com.healthcare.backend.doctor.repository.DoctorRepository;
import com.healthcare.backend.hospital.entity.Hospital;
import com.healthcare.backend.hospital.repository.HospitalRepository;
import com.healthcare.backend.patient.entity.Patient;
import com.healthcare.backend.patient.repository.PatientRepository;
import com.healthcare.backend.queue.entity.QueueEntry;
import com.healthcare.backend.queue.entity.QueueStatus;
import com.healthcare.backend.queue.repository.QueueEntryRepository;
import com.healthcare.backend.queue.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ConsultationService {

    private final ConsultationRepository consultationRepository;
    private final HospitalRepository hospitalRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final QueueEntryRepository queueEntryRepository;
    private final QueueService queueService;

    @Transactional
    public ConsultationResponse createConsultation(CreateConsultationRequest request) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();

        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Hospital not found: " + hospitalId));

        Patient patient = patientRepository.findByIdAndHospitalId(request.getPatientId(), hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Patient not found in your hospital: " + request.getPatientId()));

        Doctor doctor = resolveDoctorForRequest(request.getDoctorId(), hospitalId);

        Appointment appointment = null;
        if (request.getAppointmentId() != null) {
            appointment = appointmentRepository.findByIdAndHospitalId(request.getAppointmentId(), hospitalId)
                    .orElseThrow(() -> new NoSuchElementException("Appointment not found in your hospital: " + request.getAppointmentId()));

            if (!appointment.getPatient().getId().equals(patient.getId())) {
                throw new IllegalArgumentException("Appointment patient does not match request patient");
            }
            if (!appointment.getDoctor().getId().equals(doctor.getId())) {
                throw new IllegalArgumentException("Appointment doctor does not match request doctor");
            }
        }

        QueueEntry queueEntry = null;
        if (request.getQueueEntryId() != null) {
            queueEntry = queueEntryRepository.findByIdAndHospitalId(request.getQueueEntryId(), hospitalId)
                    .orElseThrow(() -> new NoSuchElementException("Queue entry not found in your hospital: " + request.getQueueEntryId()));

            if (!queueEntry.getPatient().getId().equals(patient.getId())) {
                throw new IllegalArgumentException("Queue entry patient does not match request patient");
            }
            if (!queueEntry.getDoctor().getId().equals(doctor.getId())) {
                throw new IllegalArgumentException("Queue entry doctor does not match request doctor");
            }
        } else if (appointment != null) {
            queueEntry = queueEntryRepository.findActiveEntryByAppointment(hospitalId, appointment.getId()).orElse(null);
        }

        Consultation consultation = new Consultation();
        consultation.setHospital(hospital);
        consultation.setDoctor(doctor);
        consultation.setPatient(patient);
        consultation.setAppointment(appointment);
        consultation.setQueueEntry(queueEntry);
        consultation.setConsultationDate(LocalDateTime.now());
        consultation.setChiefComplaint(request.getChiefComplaint());
        consultation.setSymptoms(request.getSymptoms());
        consultation.setClinicalNotes(request.getClinicalNotes());
        consultation.setDiagnosis(request.getDiagnosis());
        consultation.setTreatmentAdvice(request.getTreatmentAdvice());
        consultation.setStatus(ConsultationStatus.IN_PROGRESS);

        Consultation saved = consultationRepository.save(consultation);

        // Queue integration: transition queue entry to IN_CONSULTATION if it is in WAITING or CALLED status
        if (queueEntry != null && (queueEntry.getStatus() == QueueStatus.WAITING || queueEntry.getStatus() == QueueStatus.CALLED)) {
            queueService.startConsultation(queueEntry.getId());
        }

        return ConsultationResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public ConsultationResponse getConsultationById(Long id) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        Consultation consultation = consultationRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Consultation not found: " + id));

        validateDoctorAccessIfDoctorRole(consultation.getDoctor());
        return ConsultationResponse.fromEntity(consultation);
    }

    @Transactional(readOnly = true)
    public Page<ConsultationResponse> getConsultations(
            Long doctorId,
            Long patientId,
            String statusStr,
            LocalDate date,
            Pageable pageable
    ) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();

        var userPrincipal = SecurityUtils.getRequiredCurrentUserPrincipal();
        boolean isDoctorRole = userPrincipal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"));

        Long targetDoctorId = doctorId;
        if (isDoctorRole) {
            Doctor loggedInDoctor = doctorRepository.findByUserIdAndHospitalId(userPrincipal.getUserId(), hospitalId)
                    .orElseThrow(() -> new AccessDeniedException("Only doctors can query doctor consultations"));
            targetDoctorId = loggedInDoctor.getId();
        }

        ConsultationStatus status = null;
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                status = ConsultationStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        LocalDateTime startDate = null;
        LocalDateTime endDate = null;
        if (date != null) {
            startDate = date.atStartOfDay();
            endDate = date.atTime(LocalTime.MAX);
        }

        Page<Consultation> page = consultationRepository.findByHospitalIdWithFilters(
                hospitalId, targetDoctorId, patientId, status, startDate, endDate, pageable
        );

        return page.map(ConsultationResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<ConsultationResponse> getDoctorOwnConsultations(Pageable pageable) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        Long userId = SecurityUtils.getRequiredCurrentUserPrincipal().getUserId();

        Doctor doctor = doctorRepository.findByUserIdAndHospitalId(userId, hospitalId)
                .orElseThrow(() -> new AccessDeniedException("Only doctors can access their own consultations"));

        return consultationRepository.findByDoctorIdAndHospitalIdOrderByConsultationDateDesc(doctor.getId(), hospitalId, pageable)
                .map(ConsultationResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<ConsultationResponse> getPatientConsultations(Long patientId, Pageable pageable) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();

        Patient patient = patientRepository.findByIdAndHospitalId(patientId, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Patient not found in your hospital: " + patientId));

        return consultationRepository.findByPatientIdAndHospitalIdOrderByConsultationDateDesc(patient.getId(), hospitalId, pageable)
                .map(ConsultationResponse::fromEntity);
    }

    @Transactional
    public ConsultationResponse updateConsultation(Long id, UpdateConsultationRequest request) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        Consultation consultation = consultationRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Consultation not found: " + id));

        validateDoctorAccessIfDoctorRole(consultation.getDoctor());

        if (consultation.getStatus() != ConsultationStatus.IN_PROGRESS) {
            throw new IllegalStateException("Cannot update consultation with status: " + consultation.getStatus() + ". Only IN_PROGRESS consultations can be modified.");
        }

        if (request.getChiefComplaint() != null) consultation.setChiefComplaint(request.getChiefComplaint());
        if (request.getSymptoms() != null) consultation.setSymptoms(request.getSymptoms());
        if (request.getClinicalNotes() != null) consultation.setClinicalNotes(request.getClinicalNotes());
        if (request.getDiagnosis() != null) consultation.setDiagnosis(request.getDiagnosis());
        if (request.getTreatmentAdvice() != null) consultation.setTreatmentAdvice(request.getTreatmentAdvice());

        Consultation updated = consultationRepository.save(consultation);
        return ConsultationResponse.fromEntity(updated);
    }

    @Transactional
    public ConsultationResponse completeConsultation(Long id) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        Consultation consultation = consultationRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Consultation not found: " + id));

        validateDoctorAccessIfDoctorRole(consultation.getDoctor());

        if (consultation.getStatus() != ConsultationStatus.IN_PROGRESS) {
            throw new IllegalStateException("Cannot complete consultation with status: " + consultation.getStatus() + ". Only IN_PROGRESS consultations can be completed.");
        }

        consultation.setStatus(ConsultationStatus.COMPLETED);
        Consultation updated = consultationRepository.save(consultation);

        // Queue integration: complete the associated queue entry if active
        if (consultation.getQueueEntry() != null && consultation.getQueueEntry().getStatus() == QueueStatus.IN_CONSULTATION) {
            queueService.completeConsultation(consultation.getQueueEntry().getId());
        }

        return ConsultationResponse.fromEntity(updated);
    }

    private Doctor resolveDoctorForRequest(Long requestedDoctorId, Long hospitalId) {
        var userPrincipal = SecurityUtils.getRequiredCurrentUserPrincipal();
        boolean isDoctorRole = userPrincipal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"));

        if (isDoctorRole) {
            Doctor loggedInDoctor = doctorRepository.findByUserIdAndHospitalId(userPrincipal.getUserId(), hospitalId)
                    .orElseThrow(() -> new AccessDeniedException("Doctor profile not found for authenticated user"));

            if (requestedDoctorId != null && !requestedDoctorId.equals(loggedInDoctor.getId())) {
                throw new AccessDeniedException("Doctors can only create consultations for themselves");
            }
            return loggedInDoctor;
        }

        if (requestedDoctorId == null) {
            throw new IllegalArgumentException("Doctor ID is required for non-doctor roles");
        }

        return doctorRepository.findByIdAndHospitalId(requestedDoctorId, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Doctor not found in your hospital: " + requestedDoctorId));
    }

    private void validateDoctorAccessIfDoctorRole(Doctor doctor) {
        var userPrincipal = SecurityUtils.getRequiredCurrentUserPrincipal();
        boolean isDoctorRole = userPrincipal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"));

        if (isDoctorRole) {
            Doctor loggedInDoctor = doctorRepository.findByUserIdAndHospitalId(userPrincipal.getUserId(), userPrincipal.getHospitalId())
                    .orElse(null);

            if (loggedInDoctor != null && !loggedInDoctor.getId().equals(doctor.getId())) {
                throw new AccessDeniedException("Doctors can only access their own consultations");
            }
        }
    }
}
