package com.healthcare.backend.doctor.repository;

import com.healthcare.backend.doctor.entity.Doctor;
import com.healthcare.backend.doctor.entity.DoctorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Optional<Doctor> findByIdAndHospitalId(Long id, Long hospitalId);

    Optional<Doctor> findByUserIdAndHospitalId(Long userId, Long hospitalId);

    Optional<Doctor> findByUserId(Long userId);

    boolean existsByUserIdAndHospitalId(Long userId, Long hospitalId);

    boolean existsByUserId(Long userId);

    long countByHospitalId(Long hospitalId);

    Page<Doctor> findByHospitalId(Long hospitalId, Pageable pageable);

    Page<Doctor> findByHospitalIdAndSpecializationContainingIgnoreCase(Long hospitalId, String specialization, Pageable pageable);

    Page<Doctor> findByHospitalIdAndStatus(Long hospitalId, DoctorStatus status, Pageable pageable);

    Page<Doctor> findByHospitalIdAndSpecializationContainingIgnoreCaseAndStatus(Long hospitalId, String specialization, DoctorStatus status, Pageable pageable);
}
