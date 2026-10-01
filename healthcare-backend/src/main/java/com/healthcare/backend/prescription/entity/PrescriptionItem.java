package com.healthcare.backend.prescription.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "prescription_items",
        indexes = {
                @Index(name = "idx_prescription_item_prescription", columnList = "prescription_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class PrescriptionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;

    @Column(name = "medicine_name", nullable = false, length = 150)
    private String medicineName;

    @Column(name = "dosage", nullable = false, length = 50)
    private String dosage;

    @Column(name = "frequency", nullable = false, length = 50)
    private String frequency;

    @Column(name = "duration", nullable = false, length = 50)
    private String duration;

    @Column(name = "route", length = 50)
    private String route;

    @Column(name = "instructions", columnDefinition = "TEXT")
    private String instructions;
}
