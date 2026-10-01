package com.healthcare.backend.lab.entity;

import com.healthcare.backend.consultation.entity.Consultation;
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
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "lab_orders",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_lab_order_number_hospital", columnNames = {"order_number", "hospital_id"})
        },
        indexes = {
                @Index(name = "idx_lab_order_hospital", columnList = "hospital_id"),
                @Index(name = "idx_lab_order_patient", columnList = "patient_id"),
                @Index(name = "idx_lab_order_doctor", columnList = "doctor_id"),
                @Index(name = "idx_lab_order_consultation", columnList = "consultation_id"),
                @Index(name = "idx_lab_order_number", columnList = "order_number"),
                @Index(name = "idx_lab_order_status", columnList = "status"),
                @Index(name = "idx_lab_order_date", columnList = "ordered_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class LabOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, length = 50)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultation_id")
    private Consultation consultation;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private LabOrderStatus status = LabOrderStatus.ORDERED;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private LabOrderPriority priority = LabOrderPriority.NORMAL;

    @Column(name = "clinical_notes", length = 1000)
    private String clinicalNotes;

    @Column(name = "ordered_at", nullable = false)
    private LocalDateTime orderedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "labOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LabOrderItem> items = new ArrayList<>();
}
