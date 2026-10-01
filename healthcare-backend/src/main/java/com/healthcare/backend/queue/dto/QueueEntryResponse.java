package com.healthcare.backend.queue.dto;

import com.healthcare.backend.queue.entity.QueueEntry;
import com.healthcare.backend.queue.entity.QueuePriority;
import com.healthcare.backend.queue.entity.QueueStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class QueueEntryResponse {

    private Long id;
    private Integer tokenNumber;
    private Long hospitalId;
    private Long doctorId;
    private String doctorName;
    private Long patientId;
    private String patientName;
    private String patientCode;
    private Long appointmentId;
    private LocalDate queueDate;
    private QueuePriority priority;
    private QueueStatus status;
    private LocalDateTime checkedInAt;
    private LocalDateTime calledAt;
    private LocalDateTime consultationStartedAt;
    private LocalDateTime completedAt;
    private Integer estimatedWaitMinutes;
    private Integer actualWaitMinutes;
    private Integer consultationDurationMinutes;
    private String notes;
    private String skipReason;
    private String cancellationReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public QueueEntryResponse() {
    }

    public static QueueEntryResponse fromEntity(QueueEntry entry) {
        QueueEntryResponse dto = new QueueEntryResponse();
        dto.setId(entry.getId());
        dto.setTokenNumber(entry.getTokenNumber());
        dto.setHospitalId(entry.getHospital().getId());

        if (entry.getDoctor() != null) {
            dto.setDoctorId(entry.getDoctor().getId());
            if (entry.getDoctor().getUser() != null) {
                dto.setDoctorName(entry.getDoctor().getUser().getFirstName() + " " + entry.getDoctor().getUser().getLastName());
            }
        }

        if (entry.getPatient() != null) {
            dto.setPatientId(entry.getPatient().getId());
            dto.setPatientName(entry.getPatient().getFirstName() + " " + entry.getPatient().getLastName());
            dto.setPatientCode(entry.getPatient().getPatientCode());
        }

        if (entry.getAppointment() != null) {
            dto.setAppointmentId(entry.getAppointment().getId());
        }

        dto.setQueueDate(entry.getQueueDate());
        dto.setPriority(entry.getPriority());
        dto.setStatus(entry.getStatus());
        dto.setCheckedInAt(entry.getCheckedInAt());
        dto.setCalledAt(entry.getCalledAt());
        dto.setConsultationStartedAt(entry.getConsultationStartedAt());
        dto.setCompletedAt(entry.getCompletedAt());
        dto.setEstimatedWaitMinutes(entry.getEstimatedWaitMinutes());
        dto.setActualWaitMinutes(entry.getActualWaitMinutes());
        dto.setConsultationDurationMinutes(entry.getConsultationDurationMinutes());
        dto.setNotes(entry.getNotes());
        dto.setSkipReason(entry.getSkipReason());
        dto.setCancellationReason(entry.getCancellationReason());
        dto.setCreatedAt(entry.getCreatedAt());
        dto.setUpdatedAt(entry.getUpdatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getTokenNumber() {
        return tokenNumber;
    }

    public void setTokenNumber(Integer tokenNumber) {
        this.tokenNumber = tokenNumber;
    }

    public Long getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(Long hospitalId) {
        this.hospitalId = hospitalId;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientCode() {
        return patientCode;
    }

    public void setPatientCode(String patientCode) {
        this.patientCode = patientCode;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Long appointmentId) {
        this.appointmentId = appointmentId;
    }

    public LocalDate getQueueDate() {
        return queueDate;
    }

    public void setQueueDate(LocalDate queueDate) {
        this.queueDate = queueDate;
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
}
