package com.healthcare.backend.queue.dto;

import com.healthcare.backend.queue.entity.DoctorQueueStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateDoctorQueueStatusRequest {

    @NotNull(message = "Queue status is required")
    private DoctorQueueStatus status;

    public UpdateDoctorQueueStatusRequest() {
    }

    public DoctorQueueStatus getStatus() {
        return status;
    }

    public void setStatus(DoctorQueueStatus status) {
        this.status = status;
    }
}
