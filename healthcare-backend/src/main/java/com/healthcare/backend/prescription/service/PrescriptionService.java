package com.healthcare.backend.prescription.service;

import com.healthcare.backend.auth.security.SecurityUtils;
import com.healthcare.backend.consultation.entity.Consultation;
import com.healthcare.backend.consultation.repository.ConsultationRepository;
import com.healthcare.backend.doctor.entity.Doctor;
import com.healthcare.backend.doctor.repository.DoctorRepository;
import com.healthcare.backend.patient.entity.Patient;
import com.healthcare.backend.patient.repository.PatientRepository;
import com.healthcare.backend.prescription.dto.CreatePrescriptionRequest;
import com.healthcare.backend.prescription.dto.PrescriptionItemRequest;
import com.healthcare.backend.prescription.dto.PrescriptionResponse;
import com.healthcare.backend.prescription.dto.UpdatePrescriptionRequest;
import com.healthcare.backend.prescription.entity.Prescription;
import com.healthcare.backend.prescription.entity.PrescriptionItem;
import com.healthcare.backend.prescription.entity.PrescriptionStatus;
import com.healthcare.backend.prescription.repository.PrescriptionItemRepository;
import com.healthcare.backend.prescription.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final ConsultationRepository consultationRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    @Transactional
    public PrescriptionResponse createPrescription(CreatePrescriptionRequest request) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        var userPrincipal = SecurityUtils.getRequiredCurrentUserPrincipal();

        Doctor doctor = doctorRepository.findByUserIdAndHospitalId(userPrincipal.getUserId(), hospitalId)
                .orElseThrow(() -> new AccessDeniedException("Doctor profile not found for authenticated user"));

        Consultation consultation = consultationRepository.findByIdAndHospitalId(request.getConsultationId(), hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Consultation not found in your hospital: " + request.getConsultationId()));

        if (!consultation.getDoctor().getId().equals(doctor.getId())) {
            throw new AccessDeniedException("Doctors can only create prescriptions for their own consultations");
        }

        if (prescriptionRepository.existsByConsultationIdAndHospitalId(consultation.getId(), hospitalId)) {
            throw new IllegalStateException("Prescription already exists for consultation: " + consultation.getId());
        }

        String datePrefix = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        long count = prescriptionRepository.countByHospitalId(hospitalId) + 1;
        String prescriptionCode = String.format("RX-%s-%04d", datePrefix, count);

        Prescription prescription = new Prescription();
        prescription.setPrescriptionCode(prescriptionCode);
        prescription.setHospital(consultation.getHospital());
        prescription.setDoctor(doctor);
        prescription.setPatient(consultation.getPatient());
        prescription.setConsultation(consultation);
        prescription.setStatus(PrescriptionStatus.DRAFT);
        prescription.setNotes(request.getNotes());

        if (request.getItems() != null) {
            for (PrescriptionItemRequest itemReq : request.getItems()) {
                PrescriptionItem item = new PrescriptionItem();
                item.setMedicineName(itemReq.getMedicineName());
                item.setDosage(itemReq.getDosage());
                item.setFrequency(itemReq.getFrequency());
                item.setDuration(itemReq.getDuration());
                item.setRoute(itemReq.getRoute());
                item.setInstructions(itemReq.getInstructions());
                prescription.addItem(item);
            }
        }

        Prescription saved = prescriptionRepository.save(prescription);
        return PrescriptionResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public PrescriptionResponse getPrescriptionById(Long id) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        Prescription prescription = prescriptionRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Prescription not found: " + id));

        validateDoctorAccessIfDoctorRole(prescription.getDoctor());
        return PrescriptionResponse.fromEntity(prescription);
    }

    @Transactional(readOnly = true)
    public Page<PrescriptionResponse> getDoctorOwnPrescriptions(Pageable pageable) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        Long userId = SecurityUtils.getRequiredCurrentUserPrincipal().getUserId();

        Doctor doctor = doctorRepository.findByUserIdAndHospitalId(userId, hospitalId)
                .orElseThrow(() -> new AccessDeniedException("Only doctors can access their own prescriptions"));

        return prescriptionRepository.findByDoctorIdAndHospitalId(doctor.getId(), hospitalId, pageable)
                .map(PrescriptionResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<PrescriptionResponse> getPatientPrescriptions(Long patientId, Pageable pageable) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();

        Patient patient = patientRepository.findByIdAndHospitalId(patientId, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Patient not found in your hospital: " + patientId));

        return prescriptionRepository.findByPatientIdAndHospitalId(patient.getId(), hospitalId, pageable)
                .map(PrescriptionResponse::fromEntity);
    }

    @Transactional
    public PrescriptionResponse updatePrescription(Long id, UpdatePrescriptionRequest request) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        Prescription prescription = prescriptionRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Prescription not found: " + id));

        validateDoctorAccessIfDoctorRole(prescription.getDoctor());

        if (prescription.getStatus() != PrescriptionStatus.DRAFT) {
            throw new IllegalStateException("Cannot update prescription with status: " + prescription.getStatus() + ". Only DRAFT prescriptions can be modified.");
        }

        if (request.getNotes() != null) {
            prescription.setNotes(request.getNotes());
        }

        if (request.getItems() != null) {
            prescription.getItems().clear();
            for (PrescriptionItemRequest itemReq : request.getItems()) {
                PrescriptionItem item = new PrescriptionItem();
                item.setMedicineName(itemReq.getMedicineName());
                item.setDosage(itemReq.getDosage());
                item.setFrequency(itemReq.getFrequency());
                item.setDuration(itemReq.getDuration());
                item.setRoute(itemReq.getRoute());
                item.setInstructions(itemReq.getInstructions());
                prescription.addItem(item);
            }
        }

        Prescription updated = prescriptionRepository.save(prescription);
        return PrescriptionResponse.fromEntity(updated);
    }

    @Transactional
    public PrescriptionResponse issuePrescription(Long id) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        Prescription prescription = prescriptionRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Prescription not found: " + id));

        validateDoctorAccessIfDoctorRole(prescription.getDoctor());

        if (prescription.getStatus() != PrescriptionStatus.DRAFT) {
            throw new IllegalStateException("Cannot issue prescription with status: " + prescription.getStatus() + ". Only DRAFT prescriptions can be issued.");
        }

        prescription.setStatus(PrescriptionStatus.ISSUED);
        prescription.setIssuedAt(LocalDateTime.now());

        Prescription updated = prescriptionRepository.save(prescription);
        return PrescriptionResponse.fromEntity(updated);
    }

    @Transactional
    public PrescriptionResponse cancelPrescription(Long id) {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        Prescription prescription = prescriptionRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Prescription not found: " + id));

        validateDoctorAccessIfDoctorRole(prescription.getDoctor());

        if (prescription.getStatus() != PrescriptionStatus.DRAFT) {
            throw new IllegalStateException("Cannot cancel prescription with status: " + prescription.getStatus() + ". Only DRAFT prescriptions can be cancelled.");
        }

        prescription.setStatus(PrescriptionStatus.CANCELLED);

        Prescription updated = prescriptionRepository.save(prescription);
        return PrescriptionResponse.fromEntity(updated);
    }

    private void validateDoctorAccessIfDoctorRole(Doctor doctor) {
        var userPrincipal = SecurityUtils.getRequiredCurrentUserPrincipal();
        boolean isDoctorRole = userPrincipal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"));

        if (isDoctorRole) {
            Doctor loggedInDoctor = doctorRepository.findByUserIdAndHospitalId(userPrincipal.getUserId(), userPrincipal.getHospitalId())
                    .orElse(null);

            if (loggedInDoctor != null && !loggedInDoctor.getId().equals(doctor.getId())) {
                throw new AccessDeniedException("Doctors can only access their own prescriptions");
            }
        }
    }
}
