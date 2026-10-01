package com.healthcare.backend.lab.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "lab_order_items",
        indexes = {
                @Index(name = "idx_lab_order_item_order", columnList = "lab_order_id"),
                @Index(name = "idx_lab_order_item_test", columnList = "lab_test_id"),
                @Index(name = "idx_lab_order_item_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class LabOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lab_order_id", nullable = false)
    private LabOrder labOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lab_test_id", nullable = false)
    private LabTest labTest;

    @Column(name = "test_name_snapshot", nullable = false, length = 150)
    private String testNameSnapshot;

    @Column(name = "test_code_snapshot", nullable = false, length = 50)
    private String testCodeSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private LabOrderStatus status = LabOrderStatus.ORDERED;

    @Column(name = "notes", length = 500)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
