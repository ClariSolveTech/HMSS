package com.healthcare.backend.hospital.repository;

import com.healthcare.backend.hospital.entity.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HospitalRepository extends JpaRepository<Hospital, Long> {
    Optional<Hospital> findByCode(String code);
    boolean existsByCode(String code);
}
