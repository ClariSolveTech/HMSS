package com.healthcare.backend.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorDashboardSummaryResponse {
    private Long todayAppointmentsCount;
    private Long todayConsultationsCount;
    private Long todayPatientsCount;
    private Long completedConsultationsCount;
    private Long noShowAppointmentsCount;
    private Long cancelledAppointmentsCount;
    private Long waitingQueueCount;
    private Long inConsultationQueueCount;
    private Long completedQueueCount;
    private Long prescriptionsIssuedCount;
    private Long labOrdersCount;
    private Long completedLabOrdersCount;
    private Long pendingLabOrdersCount;
    private BigDecimal totalRevenueBilled;
    private BigDecimal totalRevenueCollected;
    private List<ConsultationOutcomeSummary> outcomesSummary;
}
