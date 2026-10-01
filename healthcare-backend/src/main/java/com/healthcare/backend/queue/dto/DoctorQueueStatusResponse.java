package com.healthcare.backend.queue.dto;

import com.healthcare.backend.doctor.entity.Doctor;
import com.healthcare.backend.queue.entity.DoctorQueueStatus;

import java.time.LocalDateTime;

public class DoctorQueueStatusResponse {

    private Long doctorId;
    private String doctorName;
    private DoctorQueueStatus status;
    private LocalDateTime updatedAt;

    public DoctorQueueStatusResponse() {
    }

    public static DoctorQueueStatusResponse fromDoctor(Doctor doctor) {
        DoctorQueueStatusResponse response = new DoctorQueueStatusResponse();
        response.setDoctorId(doctor.getId());
        if (doctor.getUser() != null) {
            response.setDoctorName(doctor.getUser().getFirstName() + " " + doctor.getUser().getLastName());
        }
        response.setStatus(doctor.getQueueStatus());
        response.setUpdatedAt(doctor.getUpdatedAt());
        return response;
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

    public DoctorQueueStatus getStatus() {
        return status;
    }

    public void setStatus(DoctorQueueStatus status) {
        this.status = status;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
