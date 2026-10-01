package com.healthcare.backend.queue.dto;

import jakarta.validation.constraints.Size;

public class UpdateQueueStatusRequest {

    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;

    public UpdateQueueStatusRequest() {
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
