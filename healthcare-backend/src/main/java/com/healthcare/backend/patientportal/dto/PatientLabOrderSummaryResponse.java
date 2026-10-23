package com.healthcare.backend.patientportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientLabOrderSummaryResponse {
    private Long id;
    private String orderNumber;
    private String doctorName;
    private String status;
    private String priority;
    private LocalDateTime orderedAt;
    private List<String> testNames;
}
