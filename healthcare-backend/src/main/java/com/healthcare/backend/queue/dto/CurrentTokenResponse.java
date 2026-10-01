package com.healthcare.backend.queue.dto;

import com.healthcare.backend.queue.entity.QueueEntry;
import com.healthcare.backend.queue.entity.QueueStatus;

import java.time.LocalDateTime;

public class CurrentTokenResponse {

    private Long id;
    private Integer tokenNumber;
    private String patientName;
    private String patientCode;
    private QueueStatus status;
    private LocalDateTime consultationStartedAt;

    public CurrentTokenResponse() {
    }

    public static CurrentTokenResponse fromEntity(QueueEntry entry) {
        if (entry == null) {
            return null;
        }
        CurrentTokenResponse response = new CurrentTokenResponse();
        response.setId(entry.getId());
        response.setTokenNumber(entry.getTokenNumber());
        if (entry.getPatient() != null) {
            response.setPatientName(entry.getPatient().getFirstName() + " " + entry.getPatient().getLastName());
            response.setPatientCode(entry.getPatient().getPatientCode());
        }
        response.setStatus(entry.getStatus());
        response.setConsultationStartedAt(entry.getConsultationStartedAt());
        return response;
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

    public QueueStatus getStatus() {
        return status;
    }

    public void setStatus(QueueStatus status) {
        this.status = status;
    }

    public LocalDateTime getConsultationStartedAt() {
        return consultationStartedAt;
    }

    public void setConsultationStartedAt(LocalDateTime consultationStartedAt) {
        this.consultationStartedAt = consultationStartedAt;
    }
}
