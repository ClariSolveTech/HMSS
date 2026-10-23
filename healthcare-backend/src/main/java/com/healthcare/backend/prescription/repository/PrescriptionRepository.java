package com.healthcare.backend.prescription.repository;

import com.healthcare.backend.prescription.entity.Prescription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    Optional<Prescription> findByIdAndHospitalId(Long id, Long hospitalId);

    Optional<Prescription> findByConsultationIdAndHospitalId(Long consultationId, Long hospitalId);

    Page<Prescription> findByPatientIdAndHospitalId(Long patientId, Long hospitalId, Pageable pageable);

    Page<Prescription> findByDoctorIdAndHospitalId(Long doctorId, Long hospitalId, Pageable pageable);

    boolean existsByConsultationIdAndHospitalId(Long consultationId, Long hospitalId);

    long countByHospitalId(Long hospitalId);

    long countByPatientIdAndHospitalId(Long patientId, Long hospitalId);
}
