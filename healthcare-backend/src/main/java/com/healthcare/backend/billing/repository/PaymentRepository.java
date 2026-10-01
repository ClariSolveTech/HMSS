package com.healthcare.backend.billing.repository;

import com.healthcare.backend.billing.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByIdAndHospitalId(Long id, Long hospitalId);

    List<Payment> findByInvoiceIdAndHospitalId(Long invoiceId, Long hospitalId);

    Optional<Payment> findByPaymentReferenceAndHospitalId(String paymentReference, Long hospitalId);

    boolean existsByPaymentReferenceAndHospitalId(String paymentReference, Long hospitalId);

    long countByHospitalId(Long hospitalId);
}
