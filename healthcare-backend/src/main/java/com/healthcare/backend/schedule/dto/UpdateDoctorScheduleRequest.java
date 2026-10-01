package com.healthcare.backend.schedule.dto;

import com.healthcare.backend.schedule.entity.ScheduleStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
@Setter
public class UpdateDoctorScheduleRequest {

    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer slotDurationMinutes;
    private LocalTime breakStartTime;
    private LocalTime breakEndTime;
    private ScheduleStatus status;
}
