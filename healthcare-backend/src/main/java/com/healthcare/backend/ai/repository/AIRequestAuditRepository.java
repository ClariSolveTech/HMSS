package com.healthcare.backend.ai.repository;

import com.healthcare.backend.ai.entity.AIRequestAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AIRequestAuditRepository extends JpaRepository<AIRequestAudit, Long> {

    List<AIRequestAudit> findByHospitalId(Long hospitalId);

    List<AIRequestAudit> findByDoctorIdAndHospitalId(Long doctorId, Long hospitalId);

    Page<AIRequestAudit> findByDoctorIdAndHospitalIdOrderByCreatedAtDesc(Long doctorId, Long hospitalId, Pageable pageable);

    List<AIRequestAudit> findByPatientIdAndHospitalId(Long patientId, Long hospitalId);
}
