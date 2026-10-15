package org.spring.canapa.backend.notification.service;

import org.spring.canapa.backend.notification.entity.Notification;
import org.spring.canapa.backend.notification.entity.NotificationStatus;
import org.spring.canapa.backend.notification.entity.NotificationType;
import org.spring.canapa.backend.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(
            NotificationRepository notificationRepository
    ) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Notification createNotification(
            UUID recipientUserId,
            UUID roomId,
            NotificationType type,
            String message
    ) {
        Notification notification = new Notification(
                recipientUserId,
                roomId,
                type,
                message
        );

        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(UUID userId) {
        return notificationRepository
                .findByRecipientUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Notification> getRoomNotifications(UUID roomId) {
        return notificationRepository
                .findByRoomIdOrderByCreatedAtDesc(roomId);
    }

    @Transactional(readOnly = true)
    public long countPendingNotifications(UUID userId) {
        return notificationRepository.countByRecipientUserIdAndStatus(
                userId,
                NotificationStatus.PENDING
        );
    }

    @Transactional
    public Notification markAsSent(UUID notificationId) {
        Notification notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Notification not found: " + notificationId
                ));

        notification.markAsSent();
        return notificationRepository.save(notification);
    }

    @Transactional
    public Notification markAsFailed(UUID notificationId) {
        Notification notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Notification not found: " + notificationId
                ));

        notification.markAsFailed();
        return notificationRepository.save(notification);
    }
}