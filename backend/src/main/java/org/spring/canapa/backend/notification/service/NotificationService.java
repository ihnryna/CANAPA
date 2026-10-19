package org.spring.canapa.backend.notification.service;

import org.spring.canapa.backend.notification.entity.Notification;
import org.spring.canapa.backend.notification.entity.NotificationStatus;
import org.spring.canapa.backend.notification.entity.NotificationType;
import org.spring.canapa.backend.notification.repository.NotificationRepository;
import org.spring.canapa.backend.room.Room;
import org.spring.canapa.backend.room.RoomRepository;
import org.spring.canapa.backend.user.User;
import org.spring.canapa.backend.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            RoomRepository roomRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
    }

    @Transactional
    public Notification createNotification(
            UUID recipientUserId,
            UUID roomId,
            NotificationType type,
            String message
    ) {
        User recipient = userRepository.findById(recipientUserId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User not found: " + recipientUserId
                ));

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Room not found: " + roomId
                ));

        Notification notification = new Notification(
                recipient,
                room,
                type,
                message
        );

        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(UUID userId) {
        return notificationRepository
                .findByRecipient_IdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Notification> getRoomNotifications(UUID roomId) {
        return notificationRepository
                .findByRoom_IdOrderByCreatedAtDesc(roomId);
    }

    @Transactional(readOnly = true)
    public long countPendingNotifications(UUID userId) {
        return notificationRepository.countByRecipient_IdAndStatus(
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