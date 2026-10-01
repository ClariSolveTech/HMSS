package com.healthcare.backend.billing.repository;

import com.healthcare.backend.billing.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByIdAndHospitalId(Long id, Long hospitalId);

    Optional<Invoice> findByInvoiceNumberAndHospitalId(String invoiceNumber, Long hospitalId);

    boolean existsByInvoiceNumberAndHospitalId(String invoiceNumber, Long hospitalId);

    Page<Invoice> findByPatientIdAndHospitalId(Long patientId, Long hospitalId, Pageable pageable);

    Page<Invoice> findByDoctorIdAndHospitalId(Long doctorId, Long hospitalId, Pageable pageable);

    Optional<Invoice> findByConsultationIdAndHospitalId(Long consultationId, Long hospitalId);

    long countByHospitalId(Long hospitalId);
}
