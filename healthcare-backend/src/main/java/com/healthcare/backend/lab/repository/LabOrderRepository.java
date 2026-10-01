package com.healthcare.backend.lab.repository;

import com.healthcare.backend.lab.entity.LabOrder;
import com.healthcare.backend.lab.entity.LabOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LabOrderRepository extends JpaRepository<LabOrder, Long> {

    Optional<LabOrder> findByIdAndHospitalId(Long id, Long hospitalId);

    Optional<LabOrder> findByOrderNumberAndHospitalId(String orderNumber, Long hospitalId);

    Page<LabOrder> findByPatientIdAndHospitalId(Long patientId, Long hospitalId, Pageable pageable);

    Page<LabOrder> findByDoctorIdAndHospitalId(Long doctorId, Long hospitalId, Pageable pageable);

    List<LabOrder> findByConsultationIdAndHospitalId(Long consultationId, Long hospitalId);

    Page<LabOrder> findByHospitalId(Long hospitalId, Pageable pageable);

    Page<LabOrder> findByHospitalIdAndStatus(Long hospitalId, LabOrderStatus status, Pageable pageable);

    Page<LabOrder> findByDoctorIdAndHospitalIdAndStatus(Long doctorId, Long hospitalId, LabOrderStatus status, Pageable pageable);

    long countByHospitalId(Long hospitalId);

    long countByHospitalIdAndStatus(Long hospitalId, LabOrderStatus status);
}
