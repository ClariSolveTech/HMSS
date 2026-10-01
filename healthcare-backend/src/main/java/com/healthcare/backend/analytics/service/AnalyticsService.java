package com.healthcare.backend.analytics.service;

import com.healthcare.backend.analytics.dto.ConsultationOutcomeSummary;
import com.healthcare.backend.analytics.dto.DailyTrendPoint;
import com.healthcare.backend.analytics.dto.DoctorDashboardSummaryResponse;
import com.healthcare.backend.analytics.repository.AnalyticsRepository;
import com.healthcare.backend.auth.security.SecurityUtils;
import com.healthcare.backend.auth.security.UserPrincipal;
import com.healthcare.backend.doctor.entity.Doctor;
import com.healthcare.backend.doctor.repository.DoctorRepository;
import com.healthcare.backend.lab.entity.LabOrderStatus;
import com.healthcare.backend.queue.entity.QueueStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final AnalyticsRepository analyticsRepository;
    private final DoctorRepository doctorRepository;

    public DoctorDashboardSummaryResponse getDoctorDashboardSummary() {
        Doctor doctor = getAuthenticatedDoctor();
        Long hospitalId = doctor.getHospital().getId();
        Long doctorId = doctor.getId();
        LocalDate today = LocalDate.now();

        Long todayAppts = analyticsRepository.countTodayAppointments(hospitalId, doctorId, today);
        Long todayConsults = analyticsRepository.countTodayConsultations(hospitalId, doctorId, today);
        Long todayPatients = analyticsRepository.countTodayPatients(hospitalId, doctorId, today);
        Long completedConsults = analyticsRepository.countCompletedConsultations(hospitalId, doctorId);
        Long noShowAppts = analyticsRepository.countNoShowAppointments(hospitalId, doctorId);
        Long cancelledAppts = analyticsRepository.countCancelledAppointments(hospitalId, doctorId);

        Long waitingQueue = analyticsRepository.countQueueByStatus(hospitalId, doctorId, today, QueueStatus.WAITING);
        Long inConsultQueue = analyticsRepository.countQueueByStatus(hospitalId, doctorId, today, QueueStatus.IN_CONSULTATION);
        Long completedQueue = analyticsRepository.countQueueByStatus(hospitalId, doctorId, today, QueueStatus.COMPLETED);

        Long prescriptionsIssued = analyticsRepository.countPrescriptionsIssued(hospitalId, doctorId);
        Long totalLabOrders = analyticsRepository.countLabOrders(hospitalId, doctorId);
        Long completedLabOrders = analyticsRepository.countLabOrdersByStatus(hospitalId, doctorId, LabOrderStatus.COMPLETED);
        Long pendingLabOrders = analyticsRepository.countPendingLabOrders(hospitalId, doctorId);

        BigDecimal billed = analyticsRepository.sumTotalRevenueBilled(hospitalId, doctorId);
        BigDecimal collected = analyticsRepository.sumTotalRevenueCollected(hospitalId, doctorId);

        List<ConsultationOutcomeSummary> outcomes = analyticsRepository.getConsultationOutcomes(hospitalId, doctorId);

        return DoctorDashboardSummaryResponse.builder()
                .todayAppointmentsCount(todayAppts)
                .todayConsultationsCount(todayConsults)
                .todayPatientsCount(todayPatients)
                .completedConsultationsCount(completedConsults)
                .noShowAppointmentsCount(noShowAppts)
                .cancelledAppointmentsCount(cancelledAppts)
                .waitingQueueCount(waitingQueue)
                .inConsultationQueueCount(inConsultQueue)
                .completedQueueCount(completedQueue)
                .prescriptionsIssuedCount(prescriptionsIssued)
                .labOrdersCount(totalLabOrders)
                .completedLabOrdersCount(completedLabOrders)
                .pendingLabOrdersCount(pendingLabOrders)
                .totalRevenueBilled(billed)
                .totalRevenueCollected(collected)
                .outcomesSummary(outcomes)
                .build();
    }

    public List<DailyTrendPoint> getDoctorAnalyticsTrends(LocalDate fromDate, LocalDate toDate) {
        Doctor doctor = getAuthenticatedDoctor();
        Long hospitalId = doctor.getHospital().getId();
        Long doctorId = doctor.getId();

        if (fromDate == null && toDate == null) {
            toDate = LocalDate.now();
            fromDate = toDate.minusDays(13); // Default 14 days
        } else if (fromDate == null) {
            fromDate = toDate.minusDays(13);
        } else if (toDate == null) {
            toDate = fromDate.plusDays(13);
        }

        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("from date cannot be after to date");
        }

        Map<LocalDate, Long> apptTrend = analyticsRepository.getDailyAppointmentCounts(hospitalId, doctorId, fromDate, toDate);
        Map<LocalDate, Long> consultTrend = analyticsRepository.getDailyConsultationCounts(hospitalId, doctorId, fromDate, toDate);
        Map<LocalDate, Long> patientTrend = analyticsRepository.getDailyPatientCounts(hospitalId, doctorId, fromDate, toDate);
        Map<LocalDate, BigDecimal> revTrend = analyticsRepository.getDailyRevenueCounts(hospitalId, doctorId, fromDate, toDate);

        List<DailyTrendPoint> points = new ArrayList<>();
        LocalDate curr = fromDate;
        while (!curr.isAfter(toDate)) {
            Long appts = apptTrend.getOrDefault(curr, 0L);
            Long consults = consultTrend.getOrDefault(curr, 0L);
            Long patients = patientTrend.getOrDefault(curr, 0L);
            BigDecimal rev = revTrend.getOrDefault(curr, BigDecimal.ZERO);

            points.add(new DailyTrendPoint(curr, appts, consults, patients, rev));
            curr = curr.plusDays(1);
        }

        return points;
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
