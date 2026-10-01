package com.healthcare.backend.lab.entity;

import com.healthcare.backend.doctor.entity.Doctor;
import com.healthcare.backend.hospital.entity.Hospital;
import com.healthcare.backend.patient.entity.Patient;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "lab_results",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_lab_result_order_item", columnNames = {"lab_order_item_id"})
        },
        indexes = {
                @Index(name = "idx_lab_result_hospital", columnList = "hospital_id"),
                @Index(name = "idx_lab_result_order_item", columnList = "lab_order_item_id"),
                @Index(name = "idx_lab_result_order", columnList = "lab_order_id"),
                @Index(name = "idx_lab_result_patient", columnList = "patient_id"),
                @Index(name = "idx_lab_result_doctor", columnList = "doctor_id"),
                @Index(name = "idx_lab_result_status", columnList = "result_status")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class LabResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lab_order_item_id", nullable = false)
    private LabOrderItem labOrderItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lab_order_id", nullable = false)
    private LabOrder labOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @Column(name = "result_value", length = 255)
    private String resultValue;

    @Column(name = "result_unit", length = 50)
    private String resultUnit;

    @Column(name = "reference_range", length = 200)
    private String referenceRange;

    @Column(name = "abnormal_flag")
    private Boolean abnormalFlag;

    @Column(name = "interpretation", length = 1000)
    private String interpretation;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", nullable = false, length = 30)
    private LabResultStatus resultStatus = LabResultStatus.PENDING;

    @Column(name = "resulted_at")
    private LocalDateTime resultedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
