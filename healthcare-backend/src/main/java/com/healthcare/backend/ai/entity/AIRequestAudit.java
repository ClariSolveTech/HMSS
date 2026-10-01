package com.healthcare.backend.ai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "ai_request_audits",
        indexes = {
                @Index(name = "idx_ai_audit_hospital", columnList = "hospital_id"),
                @Index(name = "idx_ai_audit_doctor", columnList = "doctor_id"),
                @Index(name = "idx_ai_audit_patient", columnList = "patient_id"),
                @Index(name = "idx_ai_audit_consultation", columnList = "consultation_id"),
                @Index(name = "idx_ai_audit_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class AIRequestAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hospital_id", nullable = false)
    private Long hospitalId;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "consultation_id")
    private Long consultationId;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AIAuditStatus status = AIAuditStatus.SUCCESS;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
