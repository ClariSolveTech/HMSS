package com.healthcare.backend.queue.service;

import com.healthcare.backend.queue.entity.QueueEntry;
import com.healthcare.backend.queue.repository.QueueEntryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class EstimatedWaitTimeService {

    private static final int DEFAULT_CONSULTATION_MINUTES = 15;

    private final QueueEntryRepository queueEntryRepository;

    public EstimatedWaitTimeService(QueueEntryRepository queueEntryRepository) {
        this.queueEntryRepository = queueEntryRepository;
    }

    @Transactional(readOnly = true)
    public int calculateEstimatedWaitMinutes(Long hospitalId, Long doctorId, LocalDate queueDate, int priorityVal, java.time.LocalDateTime checkedInAt) {
        long entriesAhead = queueEntryRepository.countEntriesAheadInQueue(
                hospitalId,
                doctorId,
                queueDate,
                priorityVal,
                checkedInAt
        );

        if (entriesAhead <= 0) {
            return 0;
        }

        List<QueueEntry> recentCompleted = queueEntryRepository.findRecentCompletedEntriesForDoctor(
                hospitalId,
                doctorId,
                PageRequest.of(0, 10)
        );

        int avgDuration = DEFAULT_CONSULTATION_MINUTES;
        if (recentCompleted != null && !recentCompleted.isEmpty()) {
            double sum = 0;
            int count = 0;
            for (QueueEntry entry : recentCompleted) {
                if (entry.getConsultationDurationMinutes() != null && entry.getConsultationDurationMinutes() > 0) {
                    sum += entry.getConsultationDurationMinutes();
                    count++;
                }
            }
            if (count > 0) {
                avgDuration = (int) Math.round(sum / count);
                if (avgDuration < 1) {
                    avgDuration = DEFAULT_CONSULTATION_MINUTES;
                }
            }
        }

        return (int) (avgDuration * entriesAhead);
    }
}
