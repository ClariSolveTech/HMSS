package com.healthcare.backend.notification.dto;

import com.healthcare.backend.notification.entity.Notification;
import com.healthcare.backend.notification.entity.NotificationPriority;
import com.healthcare.backend.notification.entity.NotificationStatus;
import com.healthcare.backend.notification.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;
    private NotificationType type;
    private String title;
    private String message;
    private NotificationStatus status;
    private NotificationPriority priority;
    private String referenceType;
    private Long referenceId;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;

    public static NotificationResponse fromEntity(Notification notification) {
        if (notification == null) {
            return null;
        }
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .status(notification.getStatus())
                .priority(notification.getPriority() != null ? notification.getPriority() : NotificationPriority.NORMAL)
                .referenceType(notification.getReferenceType())
                .referenceId(notification.getReferenceId())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .build();
    }
}
