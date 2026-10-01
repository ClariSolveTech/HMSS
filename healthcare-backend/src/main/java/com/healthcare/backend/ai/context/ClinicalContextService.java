package com.healthcare.backend.ai.context;

import com.healthcare.backend.ai.dto.ClinicalAssistantAction;
import com.healthcare.backend.ai.dto.ClinicalAssistantRequest;
import com.healthcare.backend.consultation.entity.Consultation;
import com.healthcare.backend.consultation.repository.ConsultationRepository;
import com.healthcare.backend.doctor.entity.Doctor;
import com.healthcare.backend.lab.entity.LabResult;
import com.healthcare.backend.lab.repository.LabResultRepository;
import com.healthcare.backend.patient.entity.Patient;
import com.healthcare.backend.patient.repository.PatientRepository;
import com.healthcare.backend.prescription.entity.Prescription;
import com.healthcare.backend.prescription.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClinicalContextService {

    private final PatientRepository patientRepository;
    private final ConsultationRepository consultationRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final LabResultRepository labResultRepository;

    public ClinicalContext buildContext(ClinicalAssistantRequest request, Doctor doctor, Long hospitalId) {
        Long targetPatientId = request.getPatientId();
        Long consultationId = request.getConsultationId();
        ClinicalAssistantAction action = request.getAction();

        Patient patient = null;
        Consultation consultation = null;

        // 1. Resolve & Validate Consultation if provided
        if (consultationId != null) {
            final Long finalConsultId = consultationId;
            consultation = consultationRepository.findByIdAndHospitalId(consultationId, hospitalId)
                    .orElseThrow(() -> new NoSuchElementException("Consultation not found in your hospital: " + finalConsultId));

            // Validate doctor ownership of the consultation
            if (!consultation.getDoctor().getId().equals(doctor.getId())) {
                throw new AccessDeniedException("Doctors can only access AI assistance for their own consultations");
            }

            // Infer patient from consultation if patientId not explicitly provided
            if (targetPatientId == null) {
                patient = consultation.getPatient();
                targetPatientId = patient.getId();
            }
        }

        // 2. Resolve & Validate Patient if provided or inferred
        if (patient == null && targetPatientId != null) {
            final Long finalPatientId = targetPatientId;
            patient = patientRepository.findByIdAndHospitalId(finalPatientId, hospitalId)
                    .orElseThrow(() -> new NoSuchElementException("Patient not found in your hospital: " + finalPatientId));
        }

        // 3. Ensure patient and consultation match if both are present
        if (consultation != null && patient != null) {
            if (!consultation.getPatient().getId().equals(patient.getId())) {
                throw new IllegalArgumentException("Consultation patient does not match request patient");
            }
        }

        // 4. Require at least patient or consultation context
        if (patient == null && consultation == null) {
            throw new IllegalArgumentException("Either patientId or consultationId is required for action: " + action);
        }

        // 5. Gather supplementary clinical data
        Pageable topFive = PageRequest.of(0, 5);
        List<Consultation> recentConsultations = List.of();
        List<LabResult> recentLabResults = List.of();
        List<Prescription> recentPrescriptions = List.of();

        if (patient != null) {
            recentConsultations = consultationRepository
                    .findByPatientIdAndHospitalIdOrderByConsultationDateDesc(patient.getId(), hospitalId, topFive)
                    .getContent();

            recentLabResults = labResultRepository
                    .findByPatientIdAndHospitalId(patient.getId(), hospitalId, topFive)
                    .getContent();

            recentPrescriptions = prescriptionRepository
                    .findByPatientIdAndHospitalId(patient.getId(), hospitalId, topFive)
                    .getContent();
        }

        return ClinicalContext.builder()
                .patient(patient)
                .consultation(consultation)
                .recentConsultations(recentConsultations)
                .recentLabResults(recentLabResults)
                .recentPrescriptions(recentPrescriptions)
                .build();
    }
}
