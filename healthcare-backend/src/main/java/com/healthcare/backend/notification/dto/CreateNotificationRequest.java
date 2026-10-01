package com.healthcare.backend.notification.dto;

import com.healthcare.backend.notification.entity.NotificationPriority;
import com.healthcare.backend.notification.entity.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateNotificationRequest {

    @NotNull(message = "Recipient user ID is required")
    private Long recipientUserId;

    @NotNull(message = "Notification type is required")
    private NotificationType type;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Message is required")
    private String message;

    private NotificationPriority priority;

    private String referenceType;

    private Long referenceId;

    private Long hospitalId;
}
