package org.spring.canapa.backend.notification.repository;

import org.spring.canapa.backend.notification.entity.Notification;
import org.spring.canapa.backend.notification.entity.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository
    extends JpaRepository<Notification, UUID> {

    List<Notification> findByRecipient_IdOrderByCreatedAtDesc(UUID userId);

    List<Notification> findByRoom_IdOrderByCreatedAtDesc(UUID roomId);

    List<Notification> findByStatus(NotificationStatus status);

    long countByRecipient_IdAndStatus(UUID userId, NotificationStatus status);
}
