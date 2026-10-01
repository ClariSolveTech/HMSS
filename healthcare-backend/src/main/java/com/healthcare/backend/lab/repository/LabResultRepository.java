package com.healthcare.backend.lab.repository;

import com.healthcare.backend.lab.entity.LabResult;
import com.healthcare.backend.lab.entity.LabResultStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LabResultRepository extends JpaRepository<LabResult, Long> {

    Optional<LabResult> findByIdAndHospitalId(Long id, Long hospitalId);

    Optional<LabResult> findByLabOrderItemIdAndHospitalId(Long labOrderItemId, Long hospitalId);

    List<LabResult> findByLabOrderIdAndHospitalId(Long labOrderId, Long hospitalId);

    Page<LabResult> findByPatientIdAndHospitalId(Long patientId, Long hospitalId, Pageable pageable);

    Page<LabResult> findByDoctorIdAndHospitalId(Long doctorId, Long hospitalId, Pageable pageable);

    @Query("SELECT r FROM LabResult r WHERE r.doctor.id = :doctorId AND r.hospital.id = :hospitalId AND r.resultStatus = 'PENDING'")
    Page<LabResult> findPendingByDoctorAndHospital(@Param("doctorId") Long doctorId, @Param("hospitalId") Long hospitalId, Pageable pageable);

    @Query("SELECT r FROM LabResult r WHERE r.patient.id = :patientId AND r.hospital.id = :hospitalId AND r.resultStatus = 'REVIEWED'")
    Page<LabResult> findReviewedByPatientAndHospital(@Param("patientId") Long patientId, @Param("hospitalId") Long hospitalId, Pageable pageable);

    long countByHospitalId(Long hospitalId);
}
