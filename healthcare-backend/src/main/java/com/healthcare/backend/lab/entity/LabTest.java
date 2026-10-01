package com.healthcare.backend.lab.entity;

import com.healthcare.backend.hospital.entity.Hospital;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "lab_tests",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_lab_test_code_hospital", columnNames = {"test_code", "hospital_id"})
        },
        indexes = {
                @Index(name = "idx_lab_test_hospital", columnList = "hospital_id"),
                @Index(name = "idx_lab_test_code", columnList = "test_code"),
                @Index(name = "idx_lab_test_category", columnList = "category"),
                @Index(name = "idx_lab_test_active", columnList = "active")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class LabTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @Column(name = "test_code", nullable = false, length = 50)
    private String testCode;

    @Column(name = "test_name", nullable = false, length = 150)
    private String testName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "sample_type", length = 100)
    private String sampleType;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "reference_range", length = 200)
    private String referenceRange;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
