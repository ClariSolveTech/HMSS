package com.healthcare.backend.lab.repository;

import com.healthcare.backend.lab.entity.LabOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LabOrderItemRepository extends JpaRepository<LabOrderItem, Long> {

    List<LabOrderItem> findByLabOrderId(Long labOrderId);

    Optional<LabOrderItem> findByIdAndLabOrderHospitalId(Long id, Long hospitalId);

    void deleteByLabOrderId(Long labOrderId);
}
