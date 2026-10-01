package com.healthcare.backend.schedule.dto;

import com.healthcare.backend.schedule.entity.DoctorSchedule;
import com.healthcare.backend.schedule.entity.ScheduleStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Builder
public class DoctorScheduleResponse {

    private Long id;
    private Long doctorId;
    private String doctorName;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer slotDurationMinutes;
    private LocalTime breakStartTime;
    private LocalTime breakEndTime;
    private ScheduleStatus status;
    private Long hospitalId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DoctorScheduleResponse fromEntity(DoctorSchedule schedule) {
        String doctorName = "";
        if (schedule.getDoctor() != null && schedule.getDoctor().getUser() != null) {
            doctorName = schedule.getDoctor().getUser().getFirstName() + " " + schedule.getDoctor().getUser().getLastName();
        }

        return DoctorScheduleResponse.builder()
                .id(schedule.getId())
                .doctorId(schedule.getDoctor() != null ? schedule.getDoctor().getId() : null)
                .doctorName(doctorName.trim())
                .dayOfWeek(schedule.getDayOfWeek())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .slotDurationMinutes(schedule.getSlotDurationMinutes())
                .breakStartTime(schedule.getBreakStartTime())
                .breakEndTime(schedule.getBreakEndTime())
                .status(schedule.getStatus())
                .hospitalId(schedule.getHospital() != null ? schedule.getHospital().getId() : null)
                .createdAt(schedule.getCreatedAt())
                .updatedAt(schedule.getUpdatedAt())
                .build();
    }
}
