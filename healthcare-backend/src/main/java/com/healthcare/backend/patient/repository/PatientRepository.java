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

    long countByHospitalId(Long hospitalId);
}
