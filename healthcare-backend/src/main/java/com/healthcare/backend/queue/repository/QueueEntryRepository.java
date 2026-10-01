package com.healthcare.backend.queue.repository;

import com.healthcare.backend.queue.entity.QueueEntry;
import com.healthcare.backend.queue.entity.QueueStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {

    Optional<QueueEntry> findByIdAndHospitalId(Long id, Long hospitalId);

    @Query("SELECT COALESCE(MAX(q.tokenNumber), 0) FROM QueueEntry q WHERE q.hospital.id = :hospitalId AND q.doctor.id = :doctorId AND q.queueDate = :queueDate")
    Integer findMaxTokenNumber(@Param("hospitalId") Long hospitalId, @Param("doctorId") Long doctorId, @Param("queueDate") LocalDate queueDate);

    @Query("SELECT q FROM QueueEntry q WHERE q.hospital.id = :hospitalId AND q.doctor.id = :doctorId AND q.patient.id = :patientId AND q.queueDate = :queueDate AND q.status IN (com.healthcare.backend.queue.entity.QueueStatus.WAITING, com.healthcare.backend.queue.entity.QueueStatus.CALLED, com.healthcare.backend.queue.entity.QueueStatus.IN_CONSULTATION)")
    Optional<QueueEntry> findActiveEntryByPatientAndDoctorAndDate(@Param("hospitalId") Long hospitalId, @Param("doctorId") Long doctorId, @Param("patientId") Long patientId, @Param("queueDate") LocalDate queueDate);

    @Query("SELECT q FROM QueueEntry q WHERE q.hospital.id = :hospitalId AND q.appointment.id = :appointmentId AND q.status IN (com.healthcare.backend.queue.entity.QueueStatus.WAITING, com.healthcare.backend.queue.entity.QueueStatus.CALLED, com.healthcare.backend.queue.entity.QueueStatus.IN_CONSULTATION)")
    Optional<QueueEntry> findActiveEntryByAppointment(@Param("hospitalId") Long hospitalId, @Param("appointmentId") Long appointmentId);

    @Query("SELECT q FROM QueueEntry q WHERE q.hospital.id = :hospitalId AND q.doctor.id = :doctorId AND q.queueDate = :queueDate AND q.status = com.healthcare.backend.queue.entity.QueueStatus.IN_CONSULTATION")
    Optional<QueueEntry> findCurrentConsultationForDoctor(@Param("hospitalId") Long hospitalId, @Param("doctorId") Long doctorId, @Param("queueDate") LocalDate queueDate);

    @Query("SELECT q FROM QueueEntry q WHERE q.hospital.id = :hospitalId AND q.doctor.id = :doctorId AND q.queueDate = :queueDate AND (q.status = com.healthcare.backend.queue.entity.QueueStatus.WAITING OR q.status = com.healthcare.backend.queue.entity.QueueStatus.CALLED) ORDER BY CASE WHEN q.priority = com.healthcare.backend.queue.entity.QueuePriority.EMERGENCY THEN 3 WHEN q.priority = com.healthcare.backend.queue.entity.QueuePriority.URGENT THEN 2 ELSE 1 END DESC, q.checkedInAt ASC, q.tokenNumber ASC")
    List<QueueEntry> findNextWaitingEntriesForDoctor(@Param("hospitalId") Long hospitalId, @Param("doctorId") Long doctorId, @Param("queueDate") LocalDate queueDate, Pageable pageable);

    @Query("SELECT COUNT(q) FROM QueueEntry q WHERE q.hospital.id = :hospitalId AND q.doctor.id = :doctorId AND q.queueDate = :queueDate AND (q.status = com.healthcare.backend.queue.entity.QueueStatus.WAITING OR q.status = com.healthcare.backend.queue.entity.QueueStatus.CALLED) AND ( (CASE WHEN q.priority = com.healthcare.backend.queue.entity.QueuePriority.EMERGENCY THEN 3 WHEN q.priority = com.healthcare.backend.queue.entity.QueuePriority.URGENT THEN 2 ELSE 1 END > :priorityVal) OR ((CASE WHEN q.priority = com.healthcare.backend.queue.entity.QueuePriority.EMERGENCY THEN 3 WHEN q.priority = com.healthcare.backend.queue.entity.QueuePriority.URGENT THEN 2 ELSE 1 END = :priorityVal) AND q.checkedInAt < :checkedInAt) )")
    long countEntriesAheadInQueue(@Param("hospitalId") Long hospitalId, @Param("doctorId") Long doctorId, @Param("queueDate") LocalDate queueDate, @Param("priorityVal") int priorityVal, @Param("checkedInAt") java.time.LocalDateTime checkedInAt);

    @Query("SELECT q FROM QueueEntry q WHERE q.hospital.id = :hospitalId AND q.doctor.id = :doctorId AND q.status = com.healthcare.backend.queue.entity.QueueStatus.COMPLETED AND q.consultationDurationMinutes IS NOT NULL ORDER BY q.completedAt DESC")
    List<QueueEntry> findRecentCompletedEntriesForDoctor(@Param("hospitalId") Long hospitalId, @Param("doctorId") Long doctorId, Pageable pageable);

    @Query("SELECT q FROM QueueEntry q WHERE q.hospital.id = :hospitalId " +
           "AND (cast(:doctorId as java.lang.Long) IS NULL OR q.doctor.id = :doctorId) " +
           "AND (cast(:queueDate as java.time.LocalDate) IS NULL OR q.queueDate = :queueDate) " +
           "AND (cast(:status as string) IS NULL OR q.status = :status) " +
           "AND (cast(:priority as string) IS NULL OR q.priority = :priority)")
    Page<QueueEntry> findByHospitalIdWithFilters(
            @Param("hospitalId") Long hospitalId,
            @Param("doctorId") Long doctorId,
            @Param("queueDate") LocalDate queueDate,
            @Param("status") QueueStatus status,
            @Param("priority") com.healthcare.backend.queue.entity.QueuePriority priority,
            Pageable pageable
    );
}


