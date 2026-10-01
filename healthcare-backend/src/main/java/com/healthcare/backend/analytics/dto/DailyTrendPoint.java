package com.healthcare.backend.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyTrendPoint {
    private LocalDate date;
    private Long appointments;
    private Long consultations;
    private Long patients;
    private BigDecimal revenue;
}
