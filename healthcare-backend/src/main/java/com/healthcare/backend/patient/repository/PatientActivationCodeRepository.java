package com.healthcare.backend.patient.repository;

import com.healthcare.backend.patient.entity.PatientActivationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PatientActivationCodeRepository extends JpaRepository<PatientActivationCode, Long> {

    Optional<PatientActivationCode> findByCodeHashAndUsedAtIsNullAndExpiresAtAfter(String codeHash, LocalDateTime now);

    @Modifying
    @Query("UPDATE PatientActivationCode pac SET pac.usedAt = :now WHERE pac.patient.id = :patientId AND pac.usedAt IS NULL")
    void invalidateExistingCodesForPatient(@Param("patientId") Long patientId, @Param("now") LocalDateTime now);
}
