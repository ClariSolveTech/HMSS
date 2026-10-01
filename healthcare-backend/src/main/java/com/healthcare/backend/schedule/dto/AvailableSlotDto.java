package com.healthcare.backend.schedule.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableSlotDto {

    private LocalTime startTime;
    private LocalTime endTime;
    private boolean available;
}
