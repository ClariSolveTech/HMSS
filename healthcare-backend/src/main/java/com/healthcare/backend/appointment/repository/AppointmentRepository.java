package com.healthcare.backend.appointment.repository;

import com.healthcare.backend.appointment.entity.Appointment;
import com.healthcare.backend.appointment.entity.AppointmentStatus;
import com.healthcare.backend.appointment.entity.AppointmentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    Optional<Appointment> findByIdAndHospitalId(Long id, Long hospitalId);

    Optional<Appointment> findByAppointmentNumberAndHospitalId(String appointmentNumber, Long hospitalId);

    boolean existsByDoctorIdAndAppointmentDateAndStartTimeAndStatusIn(
            Long doctorId, LocalDate appointmentDate, LocalTime startTime, Collection<AppointmentStatus> statuses
    );

    java.util.List<Appointment> findByDoctorIdAndAppointmentDateAndStatusIn(
            Long doctorId, LocalDate appointmentDate, Collection<AppointmentStatus> statuses
    );

    @Query("SELECT a FROM Appointment a WHERE a.hospital.id = :hospitalId " +
            "AND (cast(:doctorId as string) IS NULL OR a.doctor.id = :doctorId) " +
            "AND (cast(:patientId as string) IS NULL OR a.patient.id = :patientId) " +
            "AND (cast(:appointmentDate as string) IS NULL OR a.appointmentDate = :appointmentDate) " +
            "AND (:statusStr IS NULL OR cast(a.status as string) = :statusStr) " +
            "AND (:typeStr IS NULL OR cast(a.appointmentType as string) = :typeStr) " +
            "AND (cast(:search as string) IS NULL OR cast(:search as string) = '' " +
            "     OR LOWER(a.appointmentNumber) LIKE LOWER(CONCAT('%', cast(:search as string), '%')) " +
            "     OR LOWER(a.patient.firstName) LIKE LOWER(CONCAT('%', cast(:search as string), '%')) " +
            "     OR LOWER(a.patient.lastName) LIKE LOWER(CONCAT('%', cast(:search as string), '%')) " +
            "     OR LOWER(CONCAT(a.patient.firstName, ' ', a.patient.lastName)) LIKE LOWER(CONCAT('%', cast(:search as string), '%')))")
    Page<Appointment> findByHospitalIdWithFilters(
            @Param("hospitalId") Long hospitalId,
            @Param("doctorId") Long doctorId,
            @Param("patientId") Long patientId,
            @Param("appointmentDate") LocalDate appointmentDate,
            @Param("statusStr") String statusStr,
            @Param("typeStr") String typeStr,
            @Param("search") String search,
            Pageable pageable
    );

    Optional<Appointment> findByIdAndPatientIdAndHospitalId(Long id, Long patientId, Long hospitalId);

    @Query("SELECT a FROM Appointment a WHERE a.patient.id = :patientId AND a.hospital.id = :hospitalId " +
            "AND (:status IS NULL OR a.status = :status) " +
            "AND (:appointmentType IS NULL OR a.appointmentType = :appointmentType)")
    Page<Appointment> findByPatientIdAndHospitalIdFiltered(
            @Param("patientId") Long patientId,
            @Param("hospitalId") Long hospitalId,
            @Param("status") AppointmentStatus status,
            @Param("appointmentType") AppointmentType appointmentType,
            Pageable pageable
    );

    long countByPatientIdAndHospitalIdAndStatusInAndAppointmentDateGreaterThanEqual(
            Long patientId, Long hospitalId, Collection<AppointmentStatus> statuses, LocalDate date
    );

    Optional<Appointment> findFirstByPatientIdAndHospitalIdAndStatusInAndAppointmentDateGreaterThanEqualOrderByAppointmentDateAscStartTimeAsc(
            Long patientId, Long hospitalId, Collection<AppointmentStatus> statuses, LocalDate date
    );

    java.util.List<Appointment> findTop3ByPatientIdAndHospitalIdOrderByAppointmentDateDescStartTimeDesc(
            Long patientId, Long hospitalId
    );
}
