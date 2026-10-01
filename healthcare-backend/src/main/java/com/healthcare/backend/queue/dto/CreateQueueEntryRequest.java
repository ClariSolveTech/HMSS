package com.healthcare.backend.queue.dto;

import com.healthcare.backend.queue.entity.QueuePriority;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateQueueEntryRequest {

    @NotNull(message = "Doctor ID is required")
    private Long doctorId;

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    private Long appointmentId;

    private QueuePriority priority = QueuePriority.NORMAL;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;

    public CreateQueueEntryRequest() {
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Long appointmentId) {
        this.appointmentId = appointmentId;
    }

    public QueuePriority getPriority() {
        return priority;
    }

    public void setPriority(QueuePriority priority) {
        this.priority = priority != null ? priority : QueuePriority.NORMAL;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
