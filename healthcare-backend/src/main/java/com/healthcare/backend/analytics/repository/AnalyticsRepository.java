package com.healthcare.backend.analytics.repository;

import com.healthcare.backend.analytics.dto.ConsultationOutcomeSummary;
import com.healthcare.backend.appointment.entity.AppointmentStatus;
import com.healthcare.backend.consultation.entity.ConsultationStatus;
import com.healthcare.backend.billing.entity.InvoiceStatus;
import com.healthcare.backend.lab.entity.LabOrderStatus;
import com.healthcare.backend.prescription.entity.PrescriptionStatus;
import com.healthcare.backend.queue.entity.QueueStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class AnalyticsRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Long countTodayAppointments(Long hospitalId, Long doctorId, LocalDate today) {
        return entityManager.createQuery(
                        "SELECT COUNT(a) FROM Appointment a WHERE a.hospital.id = :hospitalId AND a.doctor.id = :doctorId AND a.appointmentDate = :today", Long.class)
                .setParameter("hospitalId", hospitalId)
                .setParameter("doctorId", doctorId)
                .setParameter("today", today)
                .getSingleResult();
    }

    public Long countTodayConsultations(Long hospitalId, Long doctorId, LocalDate today) {
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        return entityManager.createQuery(
                        "SELECT COUNT(c) FROM Consultation c WHERE c.hospital.id = :hospitalId AND c.doctor.id = :doctorId AND c.consultationDate BETWEEN :startOfDay AND :endOfDay", Long.class)
                .setParameter("hospitalId", hospitalId)
                .setParameter("doctorId", doctorId)
                .setParameter("startOfDay", startOfDay)
                .setParameter("endOfDay", endOfDay)
                .getSingleResult();
    }

    public Long countTodayPatients(Long hospitalId, Long doctorId, LocalDate today) {
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        return entityManager.createQuery(
                        "SELECT COUNT(DISTINCT c.patient.id) FROM Consultation c WHERE c.hospital.id = :hospitalId AND c.doctor.id = :doctorId AND c.consultationDate BETWEEN :startOfDay AND :endOfDay", Long.class)
                .setParameter("hospitalId", hospitalId)
                .setParameter("doctorId", doctorId)
                .setParameter("startOfDay", startOfDay)
                .setParameter("endOfDay", endOfDay)
                .getSingleResult();
    }

    public Long countCompletedConsultations(Long hospitalId, Long doctorId) {
        return entityManager.createQuery(
                        "SELECT COUNT(c) FROM Consultation c WHERE c.hospital.id = :hospitalId AND c.doctor.id = :doctorId AND c.status = :status", Long.class)
                .setParameter("hospitalId", hospitalId)
                .setParameter("doctorId", doctorId)
                .setParameter("status", ConsultationStatus.COMPLETED)
                .getSingleResult();
    }

    public Long countNoShowAppointments(Long hospitalId, Long doctorId) {
        return entityManager.createQuery(
                        "SELECT COUNT(a) FROM Appointment a WHERE a.hospital.id = :hospitalId AND a.doctor.id = :doctorId AND a.status = :status", Long.class)
                .setParameter("hospitalId", hospitalId)
                .setParameter("doctorId", doctorId)
                .setParameter("status", AppointmentStatus.NO_SHOW)
                .getSingleResult();
    }

    public Long countCancelledAppointments(Long hospitalId, Long doctorId) {
        return entityManager.createQuery(
                        "SELECT COUNT(a) FROM Appointment a WHERE a.hospital.id = :hospitalId AND a.doctor.id = :doctorId AND a.status = :status", Long.class)
                .setParameter("hospitalId", hospitalId)
                .setParameter("doctorId", doctorId)
                .setParameter("status", AppointmentStatus.CANCELLED)
                .getSingleResult();
    }

    public Long countQueueByStatus(Long hospitalId, Long doctorId, LocalDate today, QueueStatus status) {
        return entityManager.createQuery(
                        "SELECT COUNT(q) FROM QueueEntry q WHERE q.hospital.id = :hospitalId AND q.doctor.id = :doctorId AND q.queueDate = :today AND q.status = :status", Long.class)
                .setParameter("hospitalId", hospitalId)
                .setParameter("doctorId", doctorId)
                .setParameter("today", today)
                .setParameter("status", status)
                .getSingleResult();
    }

    public Long countPrescriptionsIssued(Long hospitalId, Long doctorId) {
        return entityManager.createQuery(
                        "SELECT COUNT(p) FROM Prescription p WHERE p.hospital.id = :hospitalId AND p.doctor.id = :doctorId AND p.status = :status", Long.class)
                .setParameter("hospitalId", hospitalId)
                .setParameter("doctorId", doctorId)
                .setParameter("status", PrescriptionStatus.ISSUED)
                .getSingleResult();
    }

    public Long countLabOrders(Long hospitalId, Long doctorId) {
        return entityManager.createQuery(
                        "SELECT COUNT(l) FROM LabOrder l WHERE l.hospital.id = :hospitalId AND l.doctor.id = :doctorId", Long.class)
                .setParameter("hospitalId", hospitalId)
                .setParameter("doctorId", doctorId)
                .getSingleResult();
    }

    public Long countLabOrdersByStatus(Long hospitalId, Long doctorId, LabOrderStatus status) {
        return entityManager.createQuery(
                        "SELECT COUNT(l) FROM LabOrder l WHERE l.hospital.id = :hospitalId AND l.doctor.id = :doctorId AND l.status = :status", Long.class)
                .setParameter("hospitalId", hospitalId)
                .setParameter("doctorId", doctorId)
                .setParameter("status", status)
                .getSingleResult();
    }

    public Long countPendingLabOrders(Long hospitalId, Long doctorId) {
        List<LabOrderStatus> pendingStatuses = List.of(
                LabOrderStatus.ORDERED,
                LabOrderStatus.SAMPLE_COLLECTED,
                LabOrderStatus.PROCESSING
        );
        return entityManager.createQuery(
                        "SELECT COUNT(l) FROM LabOrder l WHERE l.hospital.id = :hospitalId AND l.doctor.id = :doctorId AND l.status IN :statuses", Long.class)
                .setParameter("hospitalId", hospitalId)
                .setParameter("doctorId", doctorId)
                .setParameter("statuses", pendingStatuses)
                .getSingleResult();
    }

    public BigDecimal sumTotalRevenueBilled(Long hospitalId, Long doctorId) {
        BigDecimal sum = entityManager.createQuery(
                        "SELECT SUM(i.totalAmount) FROM Invoice i WHERE i.hospital.id = :hospitalId AND i.doctor.id = :doctorId AND i.status <> :cancelledStatus", BigDecimal.class)
                .setParameter("hospitalId", hospitalId)
                .setParameter("doctorId", doctorId)
                .setParameter("cancelledStatus", InvoiceStatus.CANCELLED)
                .getSingleResult();
        return sum != null ? sum : BigDecimal.ZERO;
    }

    public BigDecimal sumTotalRevenueCollected(Long hospitalId, Long doctorId) {
        BigDecimal sum = entityManager.createQuery(
                        "SELECT SUM(i.paidAmount) FROM Invoice i WHERE i.hospital.id = :hospitalId AND i.doctor.id = :doctorId AND i.status <> :cancelledStatus", BigDecimal.class)
                .setParameter("hospitalId", hospitalId)
                .setParameter("doctorId", doctorId)
                .setParameter("cancelledStatus", InvoiceStatus.CANCELLED)
                .getSingleResult();
        return sum != null ? sum : BigDecimal.ZERO;
    }

    public List<ConsultationOutcomeSummary> getConsultationOutcomes(Long hospitalId, Long doctorId) {
        List<Object[]> results = entityManager.createQuery(
                        "SELECT c.status, COUNT(c) FROM Consultation c WHERE c.hospital.id = :hospitalId AND c.doctor.id = :doctorId GROUP BY c.status", Object[].class)
                .setParameter("hospitalId", hospitalId)
                .setParameter("doctorId", doctorId)
                .getResultList();

        List<ConsultationOutcomeSummary> list = new ArrayList<>();
        for (Object[] row : results) {
            ConsultationStatus status = (ConsultationStatus) row[0];
            Long count = (Long) row[1];
            list.add(new ConsultationOutcomeSummary(status.name(), count));
        }
        return list;
    }

    /* Trend aggregations */

    public Map<LocalDate, Long> getDailyAppointmentCounts(Long hospitalId, Long doctorId, LocalDate fromDate, LocalDate toDate) {
        List<Object[]> results = entityManager.createQuery(
                        "SELECT a.appointmentDate, COUNT(a) FROM Appointment a WHERE a.hospital.id = :hospitalId AND a.doctor.id = :doctorId AND a.appointmentDate BETWEEN :fromDate AND :toDate GROUP BY a.appointmentDate", Object[].class)
                .setParameter("hospitalId", hospitalId)
                .setParameter("doctorId", doctorId)
                .setParameter("fromDate", fromDate)
                .setParameter("toDate", toDate)
                .getResultList();

        Map<LocalDate, Long> map = new HashMap<>();
        for (Object[] row : results) {
            map.put((LocalDate) row[0], (Long) row[1]);
        }
        return map;
    }

    public Map<LocalDate, Long> getDailyConsultationCounts(Long hospitalId, Long doctorId, LocalDate fromDate, LocalDate toDate) {
        LocalDateTime start = fromDate.atStartOfDay();
        LocalDateTime end = toDate.atTime(LocalTime.MAX);
        List<Object[]> results = entityManager.createQuery(
                        "SELECT CAST(c.consultationDate AS date), COUNT(c) FROM Consultation c WHERE c.hospital.id = :hospitalId AND c.doctor.id = :doctorId AND c.consultationDate BETWEEN :start AND :end GROUP BY CAST(c.consultationDate AS date)", Object[].class)
                .setParameter("hospitalId", hospitalId)
                .setParameter("doctorId", doctorId)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();

        Map<LocalDate, Long> map = new HashMap<>();
        for (Object[] row : results) {
            java.sql.Date d = (java.sql.Date) row[0];
            map.put(d.toLocalDate(), (Long) row[1]);
        }
        return map;
    }

    public Map<LocalDate, Long> getDailyPatientCounts(Long hospitalId, Long doctorId, LocalDate fromDate, LocalDate toDate) {
        LocalDateTime start = fromDate.atStartOfDay();
        LocalDateTime end = toDate.atTime(LocalTime.MAX);
        List<Object[]> results = entityManager.createQuery(
                        "SELECT CAST(c.consultationDate AS date), COUNT(DISTINCT c.patient.id) FROM Consultation c WHERE c.hospital.id = :hospitalId AND c.doctor.id = :doctorId AND c.consultationDate BETWEEN :start AND :end GROUP BY CAST(c.consultationDate AS date)", Object[].class)
                .setParameter("hospitalId", hospitalId)
                .setParameter("doctorId", doctorId)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();

        Map<LocalDate, Long> map = new HashMap<>();
        for (Object[] row : results) {
            java.sql.Date d = (java.sql.Date) row[0];
            map.put(d.toLocalDate(), (Long) row[1]);
        }
        return map;
    }

    public Map<LocalDate, BigDecimal> getDailyRevenueCounts(Long hospitalId, Long doctorId, LocalDate fromDate, LocalDate toDate) {
        LocalDateTime start = fromDate.atStartOfDay();
        LocalDateTime end = toDate.atTime(LocalTime.MAX);
        List<Object[]> results = entityManager.createQuery(
                        "SELECT CAST(i.createdAt AS date), SUM(i.paidAmount) FROM Invoice i WHERE i.hospital.id = :hospitalId AND i.doctor.id = :doctorId AND i.status <> :cancelledStatus AND i.createdAt BETWEEN :start AND :end GROUP BY CAST(i.createdAt AS date)", Object[].class)
                .setParameter("hospitalId", hospitalId)
                .setParameter("doctorId", doctorId)
                .setParameter("cancelledStatus", InvoiceStatus.CANCELLED)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();

        Map<LocalDate, BigDecimal> map = new HashMap<>();
        for (Object[] row : results) {
            java.sql.Date d = (java.sql.Date) row[0];
            BigDecimal rev = (BigDecimal) row[1];
            map.put(d.toLocalDate(), rev != null ? rev : BigDecimal.ZERO);
        }
        return map;
    }
}
