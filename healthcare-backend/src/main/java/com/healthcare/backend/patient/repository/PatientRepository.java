package com.healthcare.backend.patient.repository;

import com.healthcare.backend.patient.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByIdAndHospitalId(Long id, Long hospitalId);

    Page<Patient> findByHospitalId(Long hospitalId, Pageable pageable);

    boolean existsByMobileNumberAndHospitalId(String mobileNumber, Long hospitalId);

    Optional<Patient> findByPatientCodeAndHospitalId(String patientCode, Long hospitalId);

    Optional<Patient> findByUserId(Long userId);

    Optional<Patient> findByUserIdAndHospitalId(Long userId, Long hospitalId);

    boolean existsByUserId(Long userId);

    long countByHospitalId(Long hospitalId);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Patient p WHERE p.hospital.id = :hospitalId AND (" +
            "LOWER(p.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "p.mobileNumber LIKE CONCAT('%', :search, '%') OR " +
            "LOWER(p.patientCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Patient> searchPatients(
            @org.springframework.data.repository.query.Param("hospitalId") Long hospitalId,
            @org.springframework.data.repository.query.Param("search") String search,
            Pageable pageable
    );
}
