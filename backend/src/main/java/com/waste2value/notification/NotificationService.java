package com.waste2value.notification;

import com.waste2value.notification.dto.NotificationResponse;
import com.waste2value.shared.PageResponse;
import com.waste2value.shared.exception.ResourceNotFoundException;
import com.waste2value.user.User;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void sendNotification(User user, NotificationType type, String title, String message, String relatedEntityType, Long relatedEntityId) {
        if (user == null) return;
        Notification notification = new Notification(user, type, title, message, relatedEntityType, relatedEntityId);
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getMyNotifications(Long userId, Pageable pageable) {
        return PageResponse.from(
                notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                        .map(NotificationResponse::from)
        );
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public NotificationResponse markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .filter(n -> n.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
            notification = notificationRepository.save(notification);
        }

        return NotificationResponse.from(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId, Instant.now());
    }
}
