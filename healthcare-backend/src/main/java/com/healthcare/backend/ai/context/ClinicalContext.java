package com.healthcare.backend.ai.context;

import com.healthcare.backend.ai.dto.ClinicalAssistantAction;
import com.healthcare.backend.consultation.entity.Consultation;
import com.healthcare.backend.lab.entity.LabResult;
import com.healthcare.backend.patient.entity.Patient;
import com.healthcare.backend.prescription.entity.Prescription;
import com.healthcare.backend.prescription.entity.PrescriptionItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalContext {

    private Patient patient;

    private Consultation consultation;

    @Builder.Default
    private List<Consultation> recentConsultations = new ArrayList<>();

    @Builder.Default
    private List<LabResult> recentLabResults = new ArrayList<>();

    @Builder.Default
    private List<Prescription> recentPrescriptions = new ArrayList<>();

    public String buildSourceSummary() {
        StringBuilder sb = new StringBuilder();
        if (patient != null) {
            sb.append("Patient: ").append(patient.getFirstName()).append(" ").append(patient.getLastName())
              .append(" (Code: ").append(patient.getPatientCode()).append(")");
        }
        if (consultation != null) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append("Consultation ID: ").append(consultation.getId());
        }
        if (recentLabResults != null && !recentLabResults.isEmpty()) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append("Lab Results: ").append(recentLabResults.size());
        }
        if (recentPrescriptions != null && !recentPrescriptions.isEmpty()) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append("Prescriptions: ").append(recentPrescriptions.size());
        }
        return sb.length() > 0 ? sb.toString() : "Clinical Context";
    }

    public String formatForAction(ClinicalAssistantAction action) {
        StringBuilder sb = new StringBuilder();

        // 1. Patient Demographics (Included in all actions if present)
        if (patient != null) {
            sb.append("=== PATIENT INFORMATION ===\n");
            sb.append("Patient Code: ").append(patient.getPatientCode()).append("\n");
            sb.append("Gender: ").append(patient.getGender()).append("\n");
            if (patient.getDateOfBirth() != null) {
                int age = Period.between(patient.getDateOfBirth(), java.time.LocalDate.now()).getYears();
                sb.append("Age: ").append(age).append(" (DOB: ").append(patient.getDateOfBirth()).append(")\n");
            }
            if (patient.getBloodGroup() != null) {
                sb.append("Blood Group: ").append(patient.getBloodGroup()).append("\n");
            }
            sb.append("\n");
        }

        // 2. Current Consultation
        if (consultation != null && (action == ClinicalAssistantAction.SUMMARIZE_CONSULTATION
                || action == ClinicalAssistantAction.DRAFT_CLINICAL_NOTE
                || action == ClinicalAssistantAction.ANALYZE_LAB_RESULTS
                || action == ClinicalAssistantAction.SUGGEST_CLINICAL_CONSIDERATIONS
                || action == ClinicalAssistantAction.SUMMARIZE_PATIENT)) {
            sb.append("=== CURRENT CONSULTATION ===\n");
            sb.append("Date: ").append(consultation.getConsultationDate()).append("\n");
            if (consultation.getChiefComplaint() != null) {
                sb.append("Chief Complaint: ").append(consultation.getChiefComplaint()).append("\n");
            }
            if (consultation.getSymptoms() != null) {
                sb.append("Symptoms: ").append(consultation.getSymptoms()).append("\n");
            }
            if (consultation.getClinicalNotes() != null) {
                sb.append("Clinical Notes: ").append(consultation.getClinicalNotes()).append("\n");
            }
            if (consultation.getDiagnosis() != null) {
                sb.append("Diagnosis: ").append(consultation.getDiagnosis()).append("\n");
            }
            if (consultation.getTreatmentAdvice() != null) {
                sb.append("Treatment Advice: ").append(consultation.getTreatmentAdvice()).append("\n");
            }
            sb.append("Status: ").append(consultation.getStatus()).append("\n\n");
        }

        // 3. Laboratory Results
        if (recentLabResults != null && !recentLabResults.isEmpty()
                && (action == ClinicalAssistantAction.ANALYZE_LAB_RESULTS
                || action == ClinicalAssistantAction.SUMMARIZE_PATIENT
                || action == ClinicalAssistantAction.SUMMARIZE_CONSULTATION
                || action == ClinicalAssistantAction.DRAFT_CLINICAL_NOTE
                || action == ClinicalAssistantAction.SUGGEST_CLINICAL_CONSIDERATIONS)) {
            sb.append("=== LABORATORY RESULTS ===\n");
            for (LabResult r : recentLabResults) {
                String testName = (r.getLabOrderItem() != null && r.getLabOrderItem().getTestNameSnapshot() != null)
                        ? r.getLabOrderItem().getTestNameSnapshot() : "Lab Test";
                sb.append("- ").append(testName).append(": ")
                  .append(r.getResultValue() != null ? r.getResultValue() : "N/A");
                if (r.getResultUnit() != null) sb.append(" ").append(r.getResultUnit());
                if (r.getReferenceRange() != null) sb.append(" (Ref: ").append(r.getReferenceRange()).append(")");
                if (Boolean.TRUE.equals(r.getAbnormalFlag())) sb.append(" [ABNORMAL]");
                if (r.getInterpretation() != null) sb.append(" - ").append(r.getInterpretation());
                sb.append(" [Status: ").append(r.getResultStatus()).append("]\n");
            }
            sb.append("\n");
        }

        // 4. Prescription History
        if (recentPrescriptions != null && !recentPrescriptions.isEmpty()
                && (action == ClinicalAssistantAction.SUMMARIZE_PATIENT
                || action == ClinicalAssistantAction.EXPLAIN_PATIENT_HISTORY
                || action == ClinicalAssistantAction.SUMMARIZE_CONSULTATION
                || action == ClinicalAssistantAction.SUGGEST_CLINICAL_CONSIDERATIONS)) {
            sb.append("=== PRESCRIPTION HISTORY ===\n");
            for (Prescription p : recentPrescriptions) {
                sb.append("Prescription Code: ").append(p.getPrescriptionCode())
                  .append(" (Status: ").append(p.getStatus()).append(")\n");
                if (p.getItems() != null && !p.getItems().isEmpty()) {
                    for (PrescriptionItem item : p.getItems()) {
                        sb.append("  * ").append(item.getMedicineName())
                          .append(" | ").append(item.getDosage())
                          .append(" | ").append(item.getFrequency())
                          .append(" for ").append(item.getDuration());
                        if (item.getRoute() != null) sb.append(" (Route: ").append(item.getRoute()).append(")");
                        if (item.getInstructions() != null) sb.append(" - ").append(item.getInstructions());
                        sb.append("\n");
                    }
                }
            }
            sb.append("\n");
        }

        // 5. Historical Consultations
        if (recentConsultations != null && !recentConsultations.isEmpty()
                && (action == ClinicalAssistantAction.SUMMARIZE_PATIENT
                || action == ClinicalAssistantAction.EXPLAIN_PATIENT_HISTORY
                || action == ClinicalAssistantAction.DRAFT_CLINICAL_NOTE
                || action == ClinicalAssistantAction.SUGGEST_CLINICAL_CONSIDERATIONS)) {
            sb.append("=== PREVIOUS CONSULTATIONS ===\n");
            for (Consultation c : recentConsultations) {
                if (consultation != null && c.getId().equals(consultation.getId())) {
                    continue; // Skip current consultation already detailed above
                }
                sb.append("Consultation Date: ").append(c.getConsultationDate()).append("\n");
                if (c.getChiefComplaint() != null) sb.append("  Chief Complaint: ").append(c.getChiefComplaint()).append("\n");
                if (c.getDiagnosis() != null) sb.append("  Diagnosis: ").append(c.getDiagnosis()).append("\n");
                if (c.getClinicalNotes() != null) sb.append("  Notes: ").append(c.getClinicalNotes()).append("\n");
            }
            sb.append("\n");
        }

        return sb.length() > 0 ? sb.toString() : "No clinical history available.";
    }
}
