
package org.spring.canapa.backend.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.spring.canapa.backend.notification.entity.Notification;
import org.spring.canapa.backend.notification.entity.NotificationStatus;
import org.spring.canapa.backend.notification.entity.NotificationType;
import org.spring.canapa.backend.notification.repository.NotificationRepository;
import org.spring.canapa.backend.notification.service.NotificationService;
import org.spring.canapa.backend.room.Room;
import org.spring.canapa.backend.room.RoomRepository;
import org.spring.canapa.backend.user.User;
import org.spring.canapa.backend.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@Testcontainers
class NotificationServiceRepositoryTest {

    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16-alpine")
                    .withDatabaseName("canapa_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name",
                () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    private User user;
    private User anotherUser;
    private Room room;
    private UUID userId;
    private UUID roomId;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();

        user = userRepository.save(
                new User("Notification Test User", uniqueEmail())
        );

        anotherUser = userRepository.save(
                new User("Another Notification User", uniqueEmail())
        );

        room = roomRepository.save(
                new Room("Notification Test Room", user)
        );

        userId = user.getId();
        roomId = room.getId();
    }

    private String uniqueEmail() {
        return "notification-" + UUID.randomUUID() + "@example.com";
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
        assertEquals(userId, created.getRecipient().getId());
        assertEquals(roomId, created.getRoom().getId());
        assertEquals(NotificationType.VOTING_STARTED, created.getType());
        assertEquals("Voting has started", created.getMessage());
        assertEquals(NotificationStatus.PENDING, created.getStatus());
        assertNull(created.getSentAt());

        Notification saved = notificationRepository
                .findById(created.getId())
                .orElseThrow();

        assertEquals(created.getId(), saved.getId());
        assertEquals(NotificationStatus.PENDING, saved.getStatus());
        assertEquals(userId, saved.getRecipient().getId());
        assertEquals(roomId, saved.getRoom().getId());
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
                anotherUser.getId(),
                roomId,
                NotificationType.VOTE_CAST,
                "A vote was cast"
        );

        var notifications =
                notificationService.getUserNotifications(userId);

        assertEquals(2, notifications.size());
        assertTrue(notifications.stream().allMatch(
                n -> n.getRecipient().getId().equals(userId)
        ));
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

        Room anotherRoom = roomRepository.save(
                new Room("Another Notification Room", anotherUser)
        );

        notificationService.createNotification(
                anotherUser.getId(),
                anotherRoom.getId(),
                NotificationType.VOTING_ENDED,
                "Another voting has ended"
        );

        var notifications =
                notificationService.getRoomNotifications(roomId);

        assertEquals(1, notifications.size());
        assertEquals(roomId, notifications.getFirst().getRoom().getId());
    }

    @Test
    void shouldThrowWhenNotificationDoesNotExist() {
        assertThrows(
                IllegalArgumentException.class,
                () -> notificationService.markAsSent(UUID.randomUUID())
        );
    }
}
