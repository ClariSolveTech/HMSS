package com.healthcare.backend.appointment.dto;

public class CancelAppointmentRequest {

    private String reason;

    public CancelAppointmentRequest() {
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
