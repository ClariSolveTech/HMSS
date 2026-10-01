package com.healthcare.backend.analytics.controller;

import com.healthcare.backend.analytics.dto.DailyTrendPoint;
import com.healthcare.backend.analytics.dto.DoctorDashboardSummaryResponse;
import com.healthcare.backend.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/doctors/me/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/summary")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<DoctorDashboardSummaryResponse> getDoctorDashboardSummary() {
        DoctorDashboardSummaryResponse response = analyticsService.getDoctorDashboardSummary();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/trends")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<DailyTrendPoint>> getDoctorAnalyticsTrends(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        List<DailyTrendPoint> response = analyticsService.getDoctorAnalyticsTrends(from, to);
        return ResponseEntity.ok(response);
    }
}
