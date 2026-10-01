package com.healthcare.backend.consultation.entity;

import com.healthcare.backend.appointment.entity.Appointment;
import com.healthcare.backend.doctor.entity.Doctor;
import com.healthcare.backend.hospital.entity.Hospital;
import com.healthcare.backend.patient.entity.Patient;
import com.healthcare.backend.queue.entity.QueueEntry;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "consultations",
        indexes = {
                @Index(name = "idx_consultation_hospital", columnList = "hospital_id"),
                @Index(name = "idx_consultation_doctor", columnList = "doctor_id"),
                @Index(name = "idx_consultation_patient", columnList = "patient_id"),
                @Index(name = "idx_consultation_appointment", columnList = "appointment_id"),
                @Index(name = "idx_consultation_queue_entry", columnList = "queue_entry_id"),
                @Index(name = "idx_consultation_status", columnList = "status"),
                @Index(name = "idx_consultation_date", columnList = "consultation_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Consultation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "queue_entry_id")
    private QueueEntry queueEntry;

    @Column(name = "consultation_date", nullable = false)
    private LocalDateTime consultationDate;

    @Column(name = "chief_complaint", columnDefinition = "TEXT")
    private String chiefComplaint;

    @Column(name = "symptoms", columnDefinition = "TEXT")
    private String symptoms;

    @Column(name = "clinical_notes", columnDefinition = "TEXT")
    private String clinicalNotes;

    @Column(name = "diagnosis", columnDefinition = "TEXT")
    private String diagnosis;

    @Column(name = "treatment_advice", columnDefinition = "TEXT")
    private String treatmentAdvice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ConsultationStatus status = ConsultationStatus.IN_PROGRESS;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
