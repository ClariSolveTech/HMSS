package com.healthcare.backend.ai.prompt;

import com.healthcare.backend.ai.context.ClinicalContext;
import com.healthcare.backend.ai.dto.ClinicalAssistantAction;
import org.springframework.stereotype.Component;

@Component
public class ClinicalPromptBuilder {

    private static final String SYSTEM_INSTRUCTIONS = """
            You are an advanced AI Clinical Decision-Support Assistant operating within an Electronic Health Record (EHR) system.
            Your role is strictly to assist authorized clinicians by summarizing clinical history, analyzing lab trends, and providing clinical considerations.

            CRITICAL CLINICAL RULES & CONSTRAINTS:
            1. THIS IS CLINICAL DECISION-SUPPORT CONTENT ONLY. You do NOT make autonomous diagnoses, prescribe medications, or issue final treatment decisions.
            2. DO NOT INVENT FACTS. Never generate fabricated patient demographics, unobserved lab values, or non-existent medications.
            3. STATE UNCERTAINTY. If the provided clinical context is incomplete or insufficient for an inference, explicitly state that data is insufficient.
            4. DISTINGUISH OBSERVED DATA FROM INTERPRETATION. Clearly delineate documented clinical observations from potential considerations.
            5. MANDATORY CLINICIAN REVIEW. Always advise that all recommendations must be evaluated, validated, and approved by the attending doctor.
            """;

    public String buildPrompt(ClinicalAssistantAction action, ClinicalContext context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(SYSTEM_INSTRUCTIONS).append("\n");

        prompt.append("REQUESTED ACTION: ").append(action).append("\n\n");

        prompt.append("--- CLINICAL CONTEXT START ---\n");
        if (context != null) {
            prompt.append(context.formatForAction(action));
        } else {
            prompt.append("No clinical context available.\n");
        }
        prompt.append("--- CLINICAL CONTEXT END ---\n\n");

        prompt.append("INSTRUCTIONS FOR THIS ACTION:\n");
        switch (action) {
            case SUMMARIZE_PATIENT -> prompt.append("Provide a concise summary of the patient's demographics, clinical history, key lab findings, and active medications.");
            case SUMMARIZE_CONSULTATION -> prompt.append("Summarize the current consultation details including chief complaint, reported symptoms, clinical findings, and tentative diagnosis.");
            case ANALYZE_LAB_RESULTS -> prompt.append("Analyze the provided lab results. Highlight any abnormal flags, compare against reference ranges, and state relevant clinical correlations.");
            case SUGGEST_CLINICAL_CONSIDERATIONS -> prompt.append("Suggest potential differential diagnostic considerations or clinical follow-up checks based on the documented symptoms and lab findings.");
            case DRAFT_CLINICAL_NOTE -> prompt.append("Draft a structured SOAP clinical note (Subjective, Objective, Assessment, Plan) based on the consultation notes and lab data.");
            case EXPLAIN_PATIENT_HISTORY -> prompt.append("Synthesize the patient's longitudinal medical history, tracking past consultation notes and prescription records over time.");
        }

        return prompt.toString();
    }
}
