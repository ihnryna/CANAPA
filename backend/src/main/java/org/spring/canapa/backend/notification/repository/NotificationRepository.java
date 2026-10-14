package org.spring.canapa.backend.notification.repository;

import org.spring.canapa.backend.notification.entity.Notification;
import org.spring.canapa.backend.notification.entity.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository
        extends JpaRepository<Notification, UUID> {

    List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(
            UUID recipientUserId
    );

    List<Notification> findByRoomIdOrderByCreatedAtDesc(UUID roomId);

    List<Notification> findByStatus(NotificationStatus status);

    long countByRecipientUserIdAndStatus(
            UUID recipientUserId,
            NotificationStatus status
    );
}