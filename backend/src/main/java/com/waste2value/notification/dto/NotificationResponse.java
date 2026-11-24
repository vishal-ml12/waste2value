package com.waste2value.notification.dto;

import com.waste2value.notification.Notification;
import com.waste2value.notification.NotificationType;
import java.time.Instant;

public record NotificationResponse(
        Long id,
        Long userId,
        NotificationType type,
        String title,
        String message,
        String relatedEntityType,
        Long relatedEntityId,
        Instant readAt,
        boolean isRead,
        Instant createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getUser().getId(),
                n.getType(),
                n.getTitle(),
                n.getMessage(),
                n.getRelatedEntityType(),
                n.getRelatedEntityId(),
                n.getReadAt(),
                n.getReadAt() != null,
                n.getCreatedAt()
        );
    }
}
