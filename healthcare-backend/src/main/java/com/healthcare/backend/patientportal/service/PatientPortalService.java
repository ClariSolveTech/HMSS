package com.healthcare.backend.patientportal.service;

import com.healthcare.backend.appointment.entity.Appointment;
import com.healthcare.backend.appointment.entity.AppointmentStatus;
import com.healthcare.backend.appointment.entity.AppointmentType;
import com.healthcare.backend.appointment.repository.AppointmentRepository;
import com.healthcare.backend.auth.entity.User;
import com.healthcare.backend.auth.repository.UserRepository;
import com.healthcare.backend.auth.security.SecurityUtils;
import com.healthcare.backend.auth.security.UserPrincipal;
import com.healthcare.backend.consultation.entity.Consultation;
import com.healthcare.backend.consultation.repository.ConsultationRepository;
import com.healthcare.backend.lab.entity.LabOrder;
import com.healthcare.backend.lab.repository.LabOrderRepository;
import com.healthcare.backend.patient.entity.Patient;
import com.healthcare.backend.patient.repository.PatientRepository;
import com.healthcare.backend.patientportal.dto.*;
import com.healthcare.backend.prescription.entity.Prescription;
import com.healthcare.backend.prescription.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PatientPortalService {

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final ConsultationRepository consultationRepository;
    private final LabOrderRepository labOrderRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Patient getRequiredCurrentPatient() {
        UserPrincipal principal = SecurityUtils.getRequiredCurrentUserPrincipal();
        Long userId = principal.getUserId();
        Long hospitalId = principal.getHospitalId();

        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException(
                        "No patient clinical profile linked to this user account. Please contact hospital reception to link your clinical record."));

        if (patient.getHospital() == null || !patient.getHospital().getId().equals(hospitalId)) {
            throw new AccessDeniedException("Access denied: Hospital mismatch for patient account");
        }

        return patient;
    }

    @Transactional(readOnly = true)
    public PatientDashboardSummaryResponse getDashboardSummary() {
        Patient patient = getRequiredCurrentPatient();
        Long hospitalId = patient.getHospital().getId();
        LocalDate today = LocalDate.now();

        List<AppointmentStatus> activeStatuses = List.of(
                AppointmentStatus.BOOKED,
                AppointmentStatus.CONFIRMED,
                AppointmentStatus.CHECKED_IN,
                AppointmentStatus.IN_PROGRESS
        );

        long upcomingAppointmentsCount = appointmentRepository
                .countByPatientIdAndHospitalIdAndStatusInAndAppointmentDateGreaterThanEqual(
                        patient.getId(), hospitalId, activeStatuses, today
                );

        long completedConsultationsCount = consultationRepository
                .countByPatientIdAndHospitalId(patient.getId(), hospitalId);

        long activePrescriptionsCount = prescriptionRepository
                .countByPatientIdAndHospitalId(patient.getId(), hospitalId);

        long labOrdersCount = labOrderRepository
                .countByPatientIdAndHospitalId(patient.getId(), hospitalId);

        PatientAppointmentSummary nextAppointment = appointmentRepository
                .findFirstByPatientIdAndHospitalIdAndStatusInAndAppointmentDateGreaterThanEqualOrderByAppointmentDateAscStartTimeAsc(
                        patient.getId(), hospitalId, activeStatuses, today
                ).map(this::mapToAppointmentSummary).orElse(null);

        List<PatientAppointmentSummary> recentAppointments = appointmentRepository
                .findTop3ByPatientIdAndHospitalIdOrderByAppointmentDateDescStartTimeDesc(
                        patient.getId(), hospitalId
                ).stream().map(this::mapToAppointmentSummary).collect(Collectors.toList());

        String hospitalName = patient.getHospital() != null ? patient.getHospital().getName() : "Hospital";

        return PatientDashboardSummaryResponse.builder()
                .patientId(patient.getId())
                .patientCode(patient.getPatientCode())
                .firstName(patient.getFirstName())
                .lastName(patient.getLastName())
                .bloodGroup(patient.getBloodGroup() != null ? patient.getBloodGroup().name() : "UNKNOWN")
                .gender(patient.getGender() != null ? patient.getGender().name() : "OTHER")
                .dateOfBirth(patient.getDateOfBirth())
                .hospitalId(hospitalId)
                .hospitalName(hospitalName)
                .upcomingAppointmentsCount(upcomingAppointmentsCount)
                .completedConsultationsCount(completedConsultationsCount)
                .activePrescriptionsCount(activePrescriptionsCount)
                .labOrdersCount(labOrdersCount)
                .nextAppointment(nextAppointment)
                .recentAppointments(recentAppointments)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<PatientAppointmentSummary> getAppointments(
            AppointmentStatus status,
            AppointmentType appointmentType,
            Pageable pageable
    ) {
        Patient patient = getRequiredCurrentPatient();
        Long hospitalId = patient.getHospital().getId();

        return appointmentRepository
                .findByPatientIdAndHospitalIdFiltered(patient.getId(), hospitalId, status, appointmentType, pageable)
                .map(this::mapToAppointmentSummary);
    }

    @Transactional(readOnly = true)
    public PatientAppointmentDetailResponse getAppointmentDetail(Long appointmentId) {
        Patient patient = getRequiredCurrentPatient();
        Long hospitalId = patient.getHospital().getId();

        Appointment appointment = appointmentRepository.findByIdAndPatientIdAndHospitalId(appointmentId, patient.getId(), hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Appointment not found with ID: " + appointmentId));

        TeleconsultAccessResponse eligibility = evaluateTeleconsultEligibility(appointment);

        String docName = appointment.getDoctor() != null && appointment.getDoctor().getUser() != null
                ? "Dr. " + appointment.getDoctor().getUser().getFirstName() + " " + appointment.getDoctor().getUser().getLastName()
                : "Doctor";
        String spec = appointment.getDoctor() != null ? appointment.getDoctor().getSpecialization() : "";
        String hospName = appointment.getHospital() != null ? appointment.getHospital().getName() : "";

        return PatientAppointmentDetailResponse.builder()
                .id(appointment.getId())
                .appointmentNumber(appointment.getAppointmentNumber())
                .hospitalName(hospName)
                .doctorName(docName)
                .specialization(spec)
                .appointmentDate(appointment.getAppointmentDate())
                .startTime(appointment.getStartTime())
                .endTime(appointment.getEndTime())
                .status(appointment.getStatus())
                .appointmentType(appointment.getAppointmentType())
                .isTeleconsultation(appointment.isTeleconsultation())
                .canJoinConsultation(eligibility.isCanJoin())
                .joinUrl(eligibility.getJoinUrl())
                .eligibilityReason(eligibility.getReason())
                .reason(appointment.getReason())
                .notes(appointment.getNotes())
                .cancellationReason(appointment.getCancellationReason())
                .createdAt(appointment.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public TeleconsultAccessResponse getTeleconsultAccess(Long appointmentId) {
        Patient patient = getRequiredCurrentPatient();
        Long hospitalId = patient.getHospital().getId();

        Appointment appointment = appointmentRepository.findByIdAndPatientIdAndHospitalId(appointmentId, patient.getId(), hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Appointment not found with ID: " + appointmentId));

        return evaluateTeleconsultEligibility(appointment);
    }

    public TeleconsultAccessResponse evaluateTeleconsultEligibility(Appointment appointment) {
        if (!appointment.isTeleconsultation()) {
            return TeleconsultAccessResponse.builder()
                    .appointmentId(appointment.getId())
                    .appointmentNumber(appointment.getAppointmentNumber())
                    .isTeleconsultation(false)
                    .canJoin(false)
                    .joinUrl(null)
                    .reason("This appointment is an in-person consultation, not a teleconsultation.")
                    .appointmentDate(appointment.getAppointmentDate())
                    .startTime(appointment.getStartTime())
                    .endTime(appointment.getEndTime())
                    .status(appointment.getStatus())
                    .build();
        }

        if (appointment.getMeetUrl() == null || appointment.getMeetUrl().isBlank()) {
            return TeleconsultAccessResponse.builder()
                    .appointmentId(appointment.getId())
                    .appointmentNumber(appointment.getAppointmentNumber())
                    .isTeleconsultation(true)
                    .canJoin(false)
                    .joinUrl(null)
                    .reason("Meeting link is being generated by the clinic. Please check back shortly.")
                    .appointmentDate(appointment.getAppointmentDate())
                    .startTime(appointment.getStartTime())
                    .endTime(appointment.getEndTime())
                    .status(appointment.getStatus())
                    .build();
        }

        AppointmentStatus status = appointment.getStatus();
        if (status == AppointmentStatus.CANCELLED) {
            return buildIneligibleAccess(appointment, "Appointment has been cancelled.");
        }
        if (status == AppointmentStatus.COMPLETED) {
            return buildIneligibleAccess(appointment, "Consultation has already been completed.");
        }
        if (status == AppointmentStatus.NO_SHOW) {
            return buildIneligibleAccess(appointment, "Appointment was marked as no-show.");
        }
        if (status == AppointmentStatus.BOOKED) {
            return buildIneligibleAccess(appointment, "Appointment is awaiting hospital confirmation before video call opens.");
        }

        // Status is CONFIRMED, CHECKED_IN, or IN_PROGRESS
        if (status == AppointmentStatus.IN_PROGRESS) {
            return TeleconsultAccessResponse.builder()
                    .appointmentId(appointment.getId())
                    .appointmentNumber(appointment.getAppointmentNumber())
                    .isTeleconsultation(true)
                    .canJoin(true)
                    .joinUrl(appointment.getMeetUrl())
                    .reason("Consultation is in progress. You may join now.")
                    .appointmentDate(appointment.getAppointmentDate())
                    .startTime(appointment.getStartTime())
                    .endTime(appointment.getEndTime())
                    .status(status)
                    .build();
        }

        // CONFIRMED or CHECKED_IN: Check time window
        LocalDate today = LocalDate.now();
        if (appointment.getAppointmentDate().isAfter(today)) {
            return buildIneligibleAccess(appointment, "Consultation is scheduled for " +
                    appointment.getAppointmentDate() + " at " + appointment.getStartTime() +
                    ". Join link will open 15 minutes prior to the start time.");
        }
        if (appointment.getAppointmentDate().isBefore(today)) {
            return buildIneligibleAccess(appointment, "Consultation date has passed.");
        }

        // Same day: check window [-15 min from startTime, +60 min from endTime]
        LocalTime now = LocalTime.now();
        LocalTime windowStart = appointment.getStartTime().minusMinutes(15);
        LocalTime windowEnd = appointment.getEndTime().plusMinutes(60);

        if (now.isBefore(windowStart)) {
            return buildIneligibleAccess(appointment, "Consultation starts at " + appointment.getStartTime() +
                    ". Join opens at " + windowStart + " (15 minutes prior).");
        }
        if (now.isAfter(windowEnd)) {
            return buildIneligibleAccess(appointment, "Consultation window has closed.");
        }

        return TeleconsultAccessResponse.builder()
                .appointmentId(appointment.getId())
                .appointmentNumber(appointment.getAppointmentNumber())
                .isTeleconsultation(true)
                .canJoin(true)
                .joinUrl(appointment.getMeetUrl())
                .reason("Consultation is ready to join.")
                .appointmentDate(appointment.getAppointmentDate())
                .startTime(appointment.getStartTime())
                .endTime(appointment.getEndTime())
                .status(status)
                .build();
    }

    private TeleconsultAccessResponse buildIneligibleAccess(Appointment appointment, String reason) {
        return TeleconsultAccessResponse.builder()
                .appointmentId(appointment.getId())
                .appointmentNumber(appointment.getAppointmentNumber())
                .isTeleconsultation(true)
                .canJoin(false)
                .joinUrl(null)
                .reason(reason)
                .appointmentDate(appointment.getAppointmentDate())
                .startTime(appointment.getStartTime())
                .endTime(appointment.getEndTime())
                .status(appointment.getStatus())
                .build();
    }

    @Transactional(readOnly = true)
    public PatientProfileResponse getProfile() {
        Patient patient = getRequiredCurrentPatient();
        return mapToProfileResponse(patient);
    }

    @Transactional
    public PatientProfileResponse updateProfile(UpdatePatientProfileRequest req) {
        Patient patient = getRequiredCurrentPatient();

        if (req.getEmail() != null && !req.getEmail().isBlank()
                && !req.getEmail().equalsIgnoreCase(patient.getEmail())) {
            if (userRepository.existsByEmail(req.getEmail())) {
                throw new IllegalStateException("Email '" + req.getEmail() + "' is already in use");
            }
            patient.setEmail(req.getEmail().trim().toLowerCase());
            if (patient.getUser() != null) {
                patient.getUser().setEmail(req.getEmail().trim().toLowerCase());
                userRepository.save(patient.getUser());
            }
        }

        if (req.getAddress() != null) patient.setAddress(req.getAddress());
        if (req.getCity() != null) patient.setCity(req.getCity());
        if (req.getState() != null) patient.setState(req.getState());
        if (req.getPincode() != null) patient.setPincode(req.getPincode());
        if (req.getEmergencyContactName() != null) patient.setEmergencyContactName(req.getEmergencyContactName());
        if (req.getEmergencyContactNumber() != null) patient.setEmergencyContactNumber(req.getEmergencyContactNumber());

        Patient saved = patientRepository.save(patient);
        return mapToProfileResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<PatientPrescriptionSummaryResponse> getPrescriptions(Pageable pageable) {
        Patient patient = getRequiredCurrentPatient();
        Long hospitalId = patient.getHospital().getId();

        return prescriptionRepository.findByPatientIdAndHospitalId(patient.getId(), hospitalId, pageable)
                .map(this::mapToPrescriptionSummary);
    }

    @Transactional(readOnly = true)
    public PatientPrescriptionDetailResponse getPrescriptionDetail(Long prescriptionId) {
        Patient patient = getRequiredCurrentPatient();
        Long hospitalId = patient.getHospital().getId();

        Prescription prescription = prescriptionRepository.findByIdAndHospitalId(prescriptionId, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Prescription not found with ID: " + prescriptionId));

        if (!prescription.getPatient().getId().equals(patient.getId())) {
            throw new AccessDeniedException("Access denied: You do not own this prescription");
        }

        String docName = prescription.getDoctor() != null && prescription.getDoctor().getUser() != null
                ? "Dr. " + prescription.getDoctor().getUser().getFirstName() + " " + prescription.getDoctor().getUser().getLastName()
                : "Doctor";
        String spec = prescription.getDoctor() != null ? prescription.getDoctor().getSpecialization() : "";
        String diagnosis = prescription.getConsultation() != null ? prescription.getConsultation().getDiagnosis() : "";

        List<PatientPrescriptionItemResponse> items = prescription.getItems().stream()
                .map(item -> PatientPrescriptionItemResponse.builder()
                        .id(item.getId())
                        .medicineName(item.getMedicineName())
                        .dosage(item.getDosage())
                        .frequency(item.getFrequency())
                        .duration(item.getDuration())
                        .route(item.getRoute())
                        .instructions(item.getInstructions())
                        .build())
                .collect(Collectors.toList());

        return PatientPrescriptionDetailResponse.builder()
                .id(prescription.getId())
                .prescriptionCode(prescription.getPrescriptionCode())
                .doctorName(docName)
                .specialization(spec)
                .diagnosis(diagnosis)
                .issuedDate(prescription.getCreatedAt())
                .status(prescription.getStatus() != null ? prescription.getStatus().name() : "")
                .notes(prescription.getNotes())
                .items(items)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<PatientConsultationSummaryResponse> getConsultations(Pageable pageable) {
        Patient patient = getRequiredCurrentPatient();
        Long hospitalId = patient.getHospital().getId();

        return consultationRepository.findByPatientIdAndHospitalIdOrderByConsultationDateDesc(patient.getId(), hospitalId, pageable)
                .map(c -> {
                    String docName = c.getDoctor() != null && c.getDoctor().getUser() != null
                            ? "Dr. " + c.getDoctor().getUser().getFirstName() + " " + c.getDoctor().getUser().getLastName()
                            : "Doctor";
                    String spec = c.getDoctor() != null ? c.getDoctor().getSpecialization() : "";

                    return PatientConsultationSummaryResponse.builder()
                            .id(c.getId())
                            .doctorName(docName)
                            .specialization(spec)
                            .consultationDate(c.getConsultationDate())
                            .status(c.getStatus() != null ? c.getStatus().name() : "")
                            .chiefComplaint(c.getChiefComplaint())
                            .diagnosis(c.getDiagnosis())
                            .symptoms(c.getSymptoms())
                            .treatmentAdvice(c.getTreatmentAdvice())
                            .build();
                });
    }

    @Transactional(readOnly = true)
    public Page<PatientLabOrderSummaryResponse> getLabOrders(Pageable pageable) {
        Patient patient = getRequiredCurrentPatient();
        Long hospitalId = patient.getHospital().getId();

        return labOrderRepository.findByPatientIdAndHospitalId(patient.getId(), hospitalId, pageable)
                .map(lo -> {
                    String docName = lo.getDoctor() != null && lo.getDoctor().getUser() != null
                            ? "Dr. " + lo.getDoctor().getUser().getFirstName() + " " + lo.getDoctor().getUser().getLastName()
                            : "Doctor";
                    List<String> tests = lo.getItems().stream()
                            .map(item -> item.getTestNameSnapshot() != null ? item.getTestNameSnapshot() : "Lab Test")
                            .collect(Collectors.toList());

                    return PatientLabOrderSummaryResponse.builder()
                            .id(lo.getId())
                            .orderNumber(lo.getOrderNumber())
                            .doctorName(docName)
                            .status(lo.getStatus() != null ? lo.getStatus().name() : "")
                            .priority(lo.getPriority() != null ? lo.getPriority().name() : "")
                            .orderedAt(lo.getOrderedAt())
                            .testNames(tests)
                            .build();
                });
    }

    private PatientAppointmentSummary mapToAppointmentSummary(Appointment a) {
        String docName = a.getDoctor() != null && a.getDoctor().getUser() != null
                ? "Dr. " + a.getDoctor().getUser().getFirstName() + " " + a.getDoctor().getUser().getLastName()
                : "Doctor";
        String spec = a.getDoctor() != null ? a.getDoctor().getSpecialization() : "";
        boolean canJoin = evaluateTeleconsultEligibility(a).isCanJoin();

        return PatientAppointmentSummary.builder()
                .id(a.getId())
                .appointmentNumber(a.getAppointmentNumber())
                .doctorName(docName)
                .specialization(spec)
                .appointmentDate(a.getAppointmentDate())
                .startTime(a.getStartTime())
                .endTime(a.getEndTime())
                .status(a.getStatus())
                .appointmentType(a.getAppointmentType())
                .isTeleconsultation(a.isTeleconsultation())
                .canJoinConsultation(canJoin)
                .reason(a.getReason())
                .build();
    }

    private PatientPrescriptionSummaryResponse mapToPrescriptionSummary(Prescription p) {
        String docName = p.getDoctor() != null && p.getDoctor().getUser() != null
                ? "Dr. " + p.getDoctor().getUser().getFirstName() + " " + p.getDoctor().getUser().getLastName()
                : "Doctor";
        String spec = p.getDoctor() != null ? p.getDoctor().getSpecialization() : "";

        return PatientPrescriptionSummaryResponse.builder()
                .id(p.getId())
                .prescriptionCode(p.getPrescriptionCode())
                .doctorName(docName)
                .specialization(spec)
                .issuedDate(p.getCreatedAt())
                .status(p.getStatus() != null ? p.getStatus().name() : "")
                .itemCount(p.getItems() != null ? p.getItems().size() : 0)
                .notes(p.getNotes())
                .build();
    }

    private PatientProfileResponse mapToProfileResponse(Patient patient) {
        String hospName = patient.getHospital() != null ? patient.getHospital().getName() : "";
        Long hospId = patient.getHospital() != null ? patient.getHospital().getId() : null;
        Long userId = patient.getUser() != null ? patient.getUser().getId() : null;

        return PatientProfileResponse.builder()
                .id(patient.getId())
                .userId(userId)
                .patientCode(patient.getPatientCode())
                .firstName(patient.getFirstName())
                .lastName(patient.getLastName())
                .dateOfBirth(patient.getDateOfBirth())
                .gender(patient.getGender())
                .bloodGroup(patient.getBloodGroup())
                .mobileNumber(patient.getMobileNumber())
                .email(patient.getEmail())
                .address(patient.getAddress())
                .city(patient.getCity())
                .state(patient.getState())
                .pincode(patient.getPincode())
                .emergencyContactName(patient.getEmergencyContactName())
                .emergencyContactNumber(patient.getEmergencyContactNumber())
                .hospitalId(hospId)
                .hospitalName(hospName)
                .build();
    }
}
