package com.healthcare.backend.schedule.service;

import com.healthcare.backend.schedule.dto.AvailableSlotDto;
import com.healthcare.backend.schedule.entity.DoctorSchedule;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SlotGenerationService {

    public List<AvailableSlotDto> generateSlots(DoctorSchedule schedule) {
        if (schedule == null) {
            return List.of();
        }
        return generateSlots(
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getSlotDurationMinutes(),
                schedule.getBreakStartTime(),
                schedule.getBreakEndTime()
        );
    }

    public List<AvailableSlotDto> generateSlots(
            LocalTime startTime,
            LocalTime endTime,
            int slotDurationMinutes,
            LocalTime breakStartTime,
            LocalTime breakEndTime) {

        List<AvailableSlotDto> slots = new ArrayList<>();

        if (startTime == null || endTime == null || slotDurationMinutes <= 0 || !startTime.isBefore(endTime)) {
            return slots;
        }

        LocalTime current = startTime;
        while (!current.plusMinutes(slotDurationMinutes).isAfter(endTime)) {
            LocalTime slotStart = current;
            LocalTime slotEnd = current.plusMinutes(slotDurationMinutes);

            // Check if slot overlaps with break interval
            boolean overlapsBreak = false;
            if (breakStartTime != null && breakEndTime != null && breakStartTime.isBefore(breakEndTime)) {
                if (slotStart.isBefore(breakEndTime) && slotEnd.isAfter(breakStartTime)) {
                    overlapsBreak = true;
                }
            }

            if (!overlapsBreak) {
                slots.add(AvailableSlotDto.builder()
                        .startTime(slotStart)
                        .endTime(slotEnd)
                        .available(true)
                        .build());
            }

            current = slotEnd;
        }

        return slots;
    }
}
