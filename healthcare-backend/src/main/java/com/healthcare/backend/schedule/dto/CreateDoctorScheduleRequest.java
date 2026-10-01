package com.healthcare.backend.schedule.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
@Setter
public class CreateDoctorScheduleRequest {

    @NotNull(message = "doctorId is required")
    private Long doctorId;

    @NotNull(message = "dayOfWeek is required")
    private DayOfWeek dayOfWeek;

    @NotNull(message = "startTime is required")
    private LocalTime startTime;

    @NotNull(message = "endTime is required")
    private LocalTime endTime;

    @NotNull(message = "slotDurationMinutes is required")
    @Min(value = 1, message = "slotDurationMinutes must be positive")
    private Integer slotDurationMinutes;

    private LocalTime breakStartTime;
    private LocalTime breakEndTime;
}
