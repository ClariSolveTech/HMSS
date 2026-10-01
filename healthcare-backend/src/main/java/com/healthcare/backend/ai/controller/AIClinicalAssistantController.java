package com.healthcare.backend.ai.controller;

import com.healthcare.backend.ai.dto.ClinicalAssistantRequest;
import com.healthcare.backend.ai.dto.ClinicalAssistantResponse;
import com.healthcare.backend.ai.service.ClinicalAssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/doctors/me/ai")
@RequiredArgsConstructor
public class AIClinicalAssistantController {

    private final ClinicalAssistantService clinicalAssistantService;

    @PostMapping("/clinical-assistant")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ClinicalAssistantResponse> processClinicalAssistantRequest(
            @Valid @RequestBody ClinicalAssistantRequest request
    ) {
        ClinicalAssistantResponse response = clinicalAssistantService.processClinicalRequest(request);
        return ResponseEntity.ok(response);
    }
}
