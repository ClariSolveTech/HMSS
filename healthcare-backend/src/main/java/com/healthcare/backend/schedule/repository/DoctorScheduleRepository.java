package com.healthcare.backend.schedule.repository;

import com.healthcare.backend.schedule.entity.DoctorSchedule;
import com.healthcare.backend.schedule.entity.ScheduleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long> {

    Optional<DoctorSchedule> findByIdAndHospitalId(Long id, Long hospitalId);

    List<DoctorSchedule> findByDoctorIdAndHospitalId(Long doctorId, Long hospitalId);

    List<DoctorSchedule> findByDoctorIdAndHospitalIdAndDayOfWeekAndStatus(
            Long doctorId, Long hospitalId, DayOfWeek dayOfWeek, ScheduleStatus status);

    List<DoctorSchedule> findByDoctorIdAndHospitalIdAndStatus(
            Long doctorId, Long hospitalId, ScheduleStatus status);

    Page<DoctorSchedule> findByHospitalId(Long hospitalId, Pageable pageable);

    @Query("SELECT s FROM DoctorSchedule s WHERE s.hospital.id = :hospitalId " +
           "AND (:doctorId IS NULL OR s.doctor.id = :doctorId) " +
           "AND (:dayOfWeek IS NULL OR s.dayOfWeek = :dayOfWeek) " +
           "AND (:status IS NULL OR s.status = :status)")
    Page<DoctorSchedule> findByHospitalIdWithFilters(
            @Param("hospitalId") Long hospitalId,
            @Param("doctorId") Long doctorId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("status") ScheduleStatus status,
            Pageable pageable);

    @Query("SELECT s FROM DoctorSchedule s WHERE s.doctor.id = :doctorId " +
           "AND s.dayOfWeek = :dayOfWeek AND s.status = 'ACTIVE' " +
           "AND s.startTime < :endTime AND s.endTime > :startTime " +
           "AND (:excludeId IS NULL OR s.id <> :excludeId)")
    List<DoctorSchedule> findOverlappingActiveSchedules(
            @Param("doctorId") Long doctorId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") Long excludeId);
}
