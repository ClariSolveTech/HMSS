package com.healthcare.backend.consultation.repository;

import com.healthcare.backend.consultation.entity.Consultation;
import com.healthcare.backend.consultation.entity.ConsultationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    Optional<Consultation> findByIdAndHospitalId(Long id, Long hospitalId);

    Page<Consultation> findByPatientIdAndHospitalIdOrderByConsultationDateDesc(Long patientId, Long hospitalId, Pageable pageable);

    Page<Consultation> findByDoctorIdAndHospitalIdOrderByConsultationDateDesc(Long doctorId, Long hospitalId, Pageable pageable);

    @Query("""
        SELECT c FROM Consultation c
        WHERE c.hospital.id = :hospitalId
          AND (:doctorId IS NULL OR c.doctor.id = :doctorId)
          AND (:patientId IS NULL OR c.patient.id = :patientId)
          AND (:status IS NULL OR c.status = :status)
          AND (:startDate IS NULL OR c.consultationDate >= :startDate)
          AND (:endDate IS NULL OR c.consultationDate <= :endDate)
    """)
    Page<Consultation> findByHospitalIdWithFilters(
            @Param("hospitalId") Long hospitalId,
            @Param("doctorId") Long doctorId,
            @Param("patientId") Long patientId,
            @Param("status") ConsultationStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    long countByPatientIdAndHospitalId(Long patientId, Long hospitalId);
}
