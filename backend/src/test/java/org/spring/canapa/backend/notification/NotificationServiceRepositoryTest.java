package org.spring.canapa.backend.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.spring.canapa.backend.notification.entity.Notification;
import org.spring.canapa.backend.notification.entity.NotificationStatus;
import org.spring.canapa.backend.notification.entity.NotificationType;
import org.spring.canapa.backend.notification.repository.NotificationRepository;
import org.spring.canapa.backend.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class NotificationServiceRepositoryTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    private UUID userId;
    private UUID roomId;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        userId = UUID.randomUUID();
        roomId = UUID.randomUUID();
    }

    @Test
    void shouldCreateAndPersistPendingNotification() {
        Notification created = notificationService.createNotification(
                userId,
                roomId,
                NotificationType.VOTING_STARTED,
                "Voting has started"
        );

        assertNotNull(created.getId());
        assertNotNull(created.getCreatedAt());
        assertEquals(userId, created.getRecipientUserId());
        assertEquals(roomId, created.getRoomId());
        assertEquals(NotificationType.VOTING_STARTED, created.getType());
        assertEquals("Voting has started", created.getMessage());
        assertEquals(NotificationStatus.PENDING, created.getStatus());
        assertNull(created.getSentAt());

        Notification saved = notificationRepository
                .findById(created.getId())
                .orElseThrow();

        assertEquals(created.getId(), saved.getId());
        assertEquals(NotificationStatus.PENDING, saved.getStatus());
    }

    @Test
    void shouldFindNotificationsByRecipient() {
        notificationService.createNotification(
                userId,
                roomId,
                NotificationType.VOTING_STARTED,
                "Voting has started"
        );

        notificationService.createNotification(
                userId,
                roomId,
                NotificationType.VOTING_ENDED,
                "Voting has ended"
        );

        notificationService.createNotification(
                UUID.randomUUID(),
                roomId,
                NotificationType.VOTE_CAST,
                "A vote was cast"
        );

        var notifications =
                notificationService.getUserNotifications(userId);

        assertEquals(2, notifications.size());
        assertTrue(notifications.stream()
                .allMatch(n -> n.getRecipientUserId().equals(userId)));
    }

    @Test
    void shouldMarkNotificationAsSentAndPersistStatus() {
        Notification created = notificationService.createNotification(
                userId,
                roomId,
                NotificationType.VOTING_ENDED,
                "Voting has ended"
        );

        notificationService.markAsSent(created.getId());

        Notification saved = notificationRepository
                .findById(created.getId())
                .orElseThrow();

        assertEquals(NotificationStatus.SENT, saved.getStatus());
        assertNotNull(saved.getSentAt());
    }

    @Test
    void shouldMarkNotificationAsFailedAndPersistStatus() {
        Notification created = notificationService.createNotification(
                userId,
                roomId,
                NotificationType.VOTE_CAST,
                "A vote was cast"
        );

        notificationService.markAsFailed(created.getId());

        Notification saved = notificationRepository
                .findById(created.getId())
                .orElseThrow();

        assertEquals(NotificationStatus.FAILED, saved.getStatus());
        assertNull(saved.getSentAt());
    }

    @Test
    void shouldCountPendingNotifications() {
        notificationService.createNotification(
                userId,
                roomId,
                NotificationType.VOTING_STARTED,
                "Voting has started"
        );

        Notification second = notificationService.createNotification(
                userId,
                roomId,
                NotificationType.VOTE_CAST,
                "A vote was cast"
        );

        notificationService.markAsSent(second.getId());

        assertEquals(
                1,
                notificationService.countPendingNotifications(userId)
        );
    }

    @Test
    void shouldReturnNotificationsForRoom() {
        notificationService.createNotification(
                userId,
                roomId,
                NotificationType.VOTING_STARTED,
                "Voting has started"
        );

        notificationService.createNotification(
                UUID.randomUUID(),
                UUID.randomUUID(),
                NotificationType.VOTING_ENDED,
                "Another voting has ended"
        );

        var notifications =
                notificationService.getRoomNotifications(roomId);

        assertEquals(1, notifications.size());
        assertEquals(roomId, notifications.getFirst().getRoomId());
    }

    @Test
    void shouldThrowWhenNotificationDoesNotExist() {
        assertThrows(
                IllegalArgumentException.class,
                () -> notificationService.markAsSent(UUID.randomUUID())
        );
    }
}