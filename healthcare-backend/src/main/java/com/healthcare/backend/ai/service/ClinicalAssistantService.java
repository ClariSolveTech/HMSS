package com.healthcare.backend.ai.service;

import com.healthcare.backend.ai.context.ClinicalContext;
import com.healthcare.backend.ai.context.ClinicalContextService;
import com.healthcare.backend.ai.dto.ClinicalAssistantRequest;
import com.healthcare.backend.ai.dto.ClinicalAssistantResponse;
import com.healthcare.backend.ai.entity.AIAuditStatus;
import com.healthcare.backend.ai.entity.AIRequestAudit;
import com.healthcare.backend.ai.provider.AIProvider;
import com.healthcare.backend.ai.provider.CohereAIProvider;
import com.healthcare.backend.ai.provider.GeminiAIProvider;
import com.healthcare.backend.ai.provider.MockAIProvider;
import com.healthcare.backend.ai.repository.AIRequestAuditRepository;
import com.healthcare.backend.auth.security.SecurityUtils;
import com.healthcare.backend.auth.security.UserPrincipal;
import com.healthcare.backend.doctor.entity.Doctor;
import com.healthcare.backend.doctor.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ClinicalAssistantService {

    private final ClinicalContextService clinicalContextService;
    private final MockAIProvider mockAIProvider;
    private final GeminiAIProvider geminiAIProvider;
    private final CohereAIProvider cohereAIProvider;
    private final AIRequestAuditRepository auditRepository;
    private final DoctorRepository doctorRepository;

    @Value("${ai.provider:mock}")
    private String activeProviderName;

    @Transactional
    public ClinicalAssistantResponse processClinicalRequest(ClinicalAssistantRequest request) {
        Doctor doctor = getAuthenticatedDoctor();
        Long hospitalId = doctor.getHospital().getId();
        Long doctorId = doctor.getId();

        Long patientId = request.getPatientId();
        Long consultationId = request.getConsultationId();
        String actionName = request.getAction() != null ? request.getAction().name() : "UNKNOWN";

        AIProvider activeProvider = getActiveProvider();
        String providerName = activeProvider.getProviderName();

        ClinicalContext context;
        try {
            context = clinicalContextService.buildContext(request, doctor, hospitalId);
            if (context.getPatient() != null) {
                patientId = context.getPatient().getId();
            }
        } catch (Exception ex) {
            recordAudit(hospitalId, doctorId, patientId, consultationId, actionName, providerName, AIAuditStatus.FAILED);
            throw ex;
        }

        ClinicalAssistantResponse response;
        try {
            response = activeProvider.generateClinicalResponse(request.getAction(), context);
            recordAudit(hospitalId, doctorId, patientId, consultationId, actionName, providerName, AIAuditStatus.SUCCESS);
        } catch (Exception ex) {
            recordAudit(hospitalId, doctorId, patientId, consultationId, actionName, providerName, AIAuditStatus.FAILED);
            throw ex;
        }

        return response;
    }

    public AIProvider getActiveProvider() {
        if ("gemini".equalsIgnoreCase(activeProviderName)) {
            return geminiAIProvider;
        } else if ("cohere".equalsIgnoreCase(activeProviderName)) {
            return cohereAIProvider;
        }
        return mockAIProvider;
    }

    private void recordAudit(Long hospitalId, Long doctorId, Long patientId, Long consultationId,
                            String action, String provider, AIAuditStatus status) {
        try {
            AIRequestAudit audit = new AIRequestAudit();
            audit.setHospitalId(hospitalId);
            audit.setDoctorId(doctorId);
            audit.setPatientId(patientId);
            audit.setConsultationId(consultationId);
            audit.setAction(action);
            audit.setProvider(provider);
            audit.setStatus(status);
            auditRepository.save(audit);
        } catch (Exception e) {
            // Audit failure should not obscure primary application exception
            e.printStackTrace();
        }
    }

    private Doctor getAuthenticatedDoctor() {
        Long hospitalId = SecurityUtils.getCurrentHospitalId();
        UserPrincipal userPrincipal = SecurityUtils.getRequiredCurrentUserPrincipal();

        boolean isDoctorRole = userPrincipal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"));

        if (!isDoctorRole) {
            throw new AccessDeniedException("Access denied: User is not a doctor");
        }

        return doctorRepository.findByUserIdAndHospitalId(userPrincipal.getUserId(), hospitalId)
                .orElseThrow(() -> new NoSuchElementException("Doctor profile not found in your hospital"));
    }
}
