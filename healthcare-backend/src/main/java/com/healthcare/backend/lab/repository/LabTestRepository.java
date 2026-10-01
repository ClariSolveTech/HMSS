package com.healthcare.backend.lab.repository;

import com.healthcare.backend.lab.entity.LabTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LabTestRepository extends JpaRepository<LabTest, Long> {

    Optional<LabTest> findByIdAndHospitalId(Long id, Long hospitalId);

    List<LabTest> findByHospitalId(Long hospitalId);

    Page<LabTest> findByHospitalId(Long hospitalId, Pageable pageable);

    List<LabTest> findByHospitalIdAndActive(Long hospitalId, boolean active);

    boolean existsByTestCodeAndHospitalId(String testCode, Long hospitalId);

    long countByHospitalId(Long hospitalId);
}
