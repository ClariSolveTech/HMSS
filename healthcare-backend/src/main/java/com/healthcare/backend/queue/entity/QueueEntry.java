package com.healthcare.backend.queue.entity;

import com.healthcare.backend.appointment.entity.Appointment;
import com.healthcare.backend.doctor.entity.Doctor;
import com.healthcare.backend.hospital.entity.Hospital;
import com.healthcare.backend.patient.entity.Patient;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "queue_entries",
        indexes = {
                @Index(name = "idx_queue_hospital", columnList = "hospital_id"),
                @Index(name = "idx_queue_doctor", columnList = "doctor_id"),
                @Index(name = "idx_queue_patient", columnList = "patient_id"),
                @Index(name = "idx_queue_date", columnList = "queue_date"),
                @Index(name = "idx_queue_status", columnList = "status"),
                @Index(name = "idx_queue_doc_date_status", columnList = "doctor_id, queue_date, status"),
                @Index(name = "idx_queue_hosp_doc_date", columnList = "hospital_id, doctor_id, queue_date")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_hosp_doc_date_token", columnNames = {"hospital_id", "doctor_id", "queue_date", "token_number"}),
                @UniqueConstraint(name = "uk_active_patient_queue", columnNames = {"active_queue_key"})
        }
)
public class QueueEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false, updatable = false)
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

    @Column(name = "queue_date", nullable = false)
    private LocalDate queueDate;

    @Column(name = "token_number", nullable = false)
    private Integer tokenNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private QueuePriority priority = QueuePriority.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private QueueStatus status = QueueStatus.WAITING;

    @Column(name = "checked_in_at", nullable = false)
    private LocalDateTime checkedInAt;

    @Column(name = "called_at")
    private LocalDateTime calledAt;

    @Column(name = "consultation_started_at")
    private LocalDateTime consultationStartedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "estimated_wait_minutes")
    private Integer estimatedWaitMinutes;

    @Column(name = "actual_wait_minutes")
    private Integer actualWaitMinutes;

    @Column(name = "consultation_duration_minutes")
    private Integer consultationDurationMinutes;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "skip_reason", length = 500)
    private String skipReason;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    /**
     * Active queue key for concurrency control:
     * "HOSP_{hospitalId}_DOC_{doctorId}_PAT_{patientId}_{queueDate}" for WAITING/CALLED/IN_CONSULTATION entries.
     * Set to null when status transitions to COMPLETED, SKIPPED, or CANCELLED so patient can be re-queued if needed.
     */
    @Column(name = "active_queue_key", length = 150)
    private String activeQueueKey;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public QueueEntry() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Hospital getHospital() {
        return hospital;
    }

    public void setHospital(Hospital hospital) {
        this.hospital = hospital;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Appointment getAppointment() {
        return appointment;
    }

    public void setAppointment(Appointment appointment) {
        this.appointment = appointment;
    }

    public LocalDate getQueueDate() {
        return queueDate;
    }

    public void setQueueDate(LocalDate queueDate) {
        this.queueDate = queueDate;
    }

    public Integer getTokenNumber() {
        return tokenNumber;
    }

    public void setTokenNumber(Integer tokenNumber) {
        this.tokenNumber = tokenNumber;
    }

    public QueuePriority getPriority() {
        return priority;
    }

    public void setPriority(QueuePriority priority) {
        this.priority = priority;
    }

    public QueueStatus getStatus() {
        return status;
    }

    public void setStatus(QueueStatus status) {
        this.status = status;
    }

    public LocalDateTime getCheckedInAt() {
        return checkedInAt;
    }

    public void setCheckedInAt(LocalDateTime checkedInAt) {
        this.checkedInAt = checkedInAt;
    }

    public LocalDateTime getCalledAt() {
        return calledAt;
    }

    public void setCalledAt(LocalDateTime calledAt) {
        this.calledAt = calledAt;
    }

    public LocalDateTime getConsultationStartedAt() {
        return consultationStartedAt;
    }

    public void setConsultationStartedAt(LocalDateTime consultationStartedAt) {
        this.consultationStartedAt = consultationStartedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Integer getEstimatedWaitMinutes() {
        return estimatedWaitMinutes;
    }

    public void setEstimatedWaitMinutes(Integer estimatedWaitMinutes) {
        this.estimatedWaitMinutes = estimatedWaitMinutes;
    }

    public Integer getActualWaitMinutes() {
        return actualWaitMinutes;
    }

    public void setActualWaitMinutes(Integer actualWaitMinutes) {
        this.actualWaitMinutes = actualWaitMinutes;
    }

    public Integer getConsultationDurationMinutes() {
        return consultationDurationMinutes;
    }

    public void setConsultationDurationMinutes(Integer consultationDurationMinutes) {
        this.consultationDurationMinutes = consultationDurationMinutes;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getSkipReason() {
        return skipReason;
    }

    public void setSkipReason(String skipReason) {
        this.skipReason = skipReason;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public String getActiveQueueKey() {
        return activeQueueKey;
    }

    public void setActiveQueueKey(String activeQueueKey) {
        this.activeQueueKey = activeQueueKey;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static String buildActiveQueueKey(Long hospitalId, Long doctorId, Long patientId, LocalDate queueDate) {
        return "HOSP_" + hospitalId + "_DOC_" + doctorId + "_PAT_" + patientId + "_" + queueDate.toString();
    }
}
