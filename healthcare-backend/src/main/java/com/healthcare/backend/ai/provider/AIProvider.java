package com.healthcare.backend.ai.provider;

import com.healthcare.backend.ai.context.ClinicalContext;
import com.healthcare.backend.ai.dto.ClinicalAssistantAction;
import com.healthcare.backend.ai.dto.ClinicalAssistantResponse;

public interface AIProvider {

    ClinicalAssistantResponse generateClinicalResponse(ClinicalAssistantAction action, ClinicalContext context);

    String getProviderName();
}
