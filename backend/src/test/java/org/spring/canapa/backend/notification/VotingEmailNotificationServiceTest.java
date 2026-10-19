package org.spring.canapa.backend.notification;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.spring.canapa.backend.notification.entity.Notification;
import org.spring.canapa.backend.notification.entity.NotificationType;
import org.spring.canapa.backend.notification.event.VotingResultsReadyEvent;
import org.spring.canapa.backend.notification.event.VotingStartedEvent;
import org.spring.canapa.backend.notification.service.EmailService;
import org.spring.canapa.backend.notification.service.NotificationService;
import org.spring.canapa.backend.notification.service.VotingEmailNotificationService;
import org.spring.canapa.backend.room.Room;
import org.spring.canapa.backend.room.RoomRepository;
import org.spring.canapa.backend.user.User;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VotingEmailNotificationServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private NotificationService notificationService;

    private VotingEmailNotificationService notificationListener;

    private UUID roomId;
    private User alice;
    private User bob;
    private Room room;

    @BeforeEach
    void setUp() {
        notificationListener = new VotingEmailNotificationService(
                roomRepository,
                emailService,
                notificationService
        );

        roomId = UUID.randomUUID();

        alice = createUser("Alice", "alice@example.com");
        bob = createUser("Bob", "bob@example.com");

        room = new Room("Friday movies", alice);

        ReflectionTestUtils.setField(room, "id", roomId);
        ReflectionTestUtils.setField(
                room,
                "participants",
                new LinkedHashSet<>(List.of(alice, bob))
        );

        when(roomRepository.findWithParticipantsById(roomId))
                .thenReturn(Optional.of(room));

        when(notificationService.createNotification(
                any(UUID.class),
                any(UUID.class),
                any(NotificationType.class),
                anyString()
        )).thenAnswer(invocation -> {
            UUID recipientId = invocation.getArgument(0);
            UUID requestedRoomId = invocation.getArgument(1);
            NotificationType type = invocation.getArgument(2);
            String message = invocation.getArgument(3);

            User recipient;

            if (recipientId.equals(alice.getId())) {
                recipient = alice;
            } else if (recipientId.equals(bob.getId())) {
                recipient = bob;
            } else {
                throw new AssertionError(
                        "Unexpected recipient ID: " + recipientId
                );
            }

            if (!requestedRoomId.equals(roomId)) {
                throw new AssertionError(
                        "Unexpected room ID: " + requestedRoomId
                );
            }

            Notification notification = new Notification(recipient, room, type, message);

            ReflectionTestUtils.setField(notification,"id",UUID.randomUUID());
            return notification;
        });
    }

    @Test
    void votingStarted_shouldEmailEveryParticipantAndMarkNotificationsSent() {
        notificationListener.onVotingStarted(
                new VotingStartedEvent(roomId)
        );

        verify(emailService, times(2))
                .sendTextEmail(anyString(), anyString(), anyString());

        verify(emailService).sendTextEmail(
                eq("alice@example.com"),
                contains("голосування розпочалося"),
                contains("Friday movies")
        );

        verify(emailService).sendTextEmail(
                eq("bob@example.com"),
                contains("голосування розпочалося"),
                contains("Friday movies")
        );

        verify(notificationService, times(2)).createNotification(
                any(UUID.class),
                eq(roomId),
                eq(NotificationType.VOTING_STARTED),
                contains("Friday movies")
        );

        verify(notificationService, times(2))
                .markAsSent(any(UUID.class));

        verify(notificationService, never())
                .markAsFailed(any(UUID.class));
    }

    @Test
    void votingResults_shouldEmailEveryParticipantAndIncludeAllTieWinners() {
        List<Long> winners = List.of(11L, 22L);

        notificationListener.onVotingResultsReady(
                new VotingResultsReadyEvent(roomId, winners, 3)
        );

        ArgumentCaptor<String> bodyCaptor =
                ArgumentCaptor.forClass(String.class);

        verify(emailService, times(2)).sendTextEmail(
                anyString(),
                eq("CANAPA — результати голосування"),
                bodyCaptor.capture()
        );

        assertEquals(2, bodyCaptor.getAllValues().size());

        for (String body : bodyCaptor.getAllValues()) {
            assertTrue(body.contains("Friday movies"));
            assertTrue(body.contains("Нічия"));
            assertTrue(body.contains("11"));
            assertTrue(body.contains("22"));
            assertTrue(body.contains("3"));
        }

        verify(notificationService, times(2)).createNotification(
                any(UUID.class),
                eq(roomId),
                eq(NotificationType.RESULTS_READY),
                contains("Нічия")
        );

        verify(notificationService, times(2))
                .markAsSent(any(UUID.class));

        verify(notificationService, never())
                .markAsFailed(any(UUID.class));
    }

    @Test
    void votingResultsWithoutVotes_shouldSendEmailWithoutWinners() {
        notificationListener.onVotingResultsReady(
                new VotingResultsReadyEvent(roomId, List.of(), 0)
        );

        verify(emailService, times(2)).sendTextEmail(
                anyString(),
                anyString(),
                contains("немає переможців")
        );

        verify(notificationService, times(2))
                .markAsSent(any(UUID.class));
    }

    @Test
    void smtpFailure_shouldMarkNotificationsFailedAndContinueSending() {
        doThrow(new RuntimeException("SMTP unavailable"))
                .when(emailService)
                .sendTextEmail(anyString(), anyString(), anyString());

        assertDoesNotThrow(() ->
                notificationListener.onVotingStarted(
                        new VotingStartedEvent(roomId)
                )
        );

        // Сервіс спробував надіслати лист кожному учаснику.
        verify(emailService, times(2))
                .sendTextEmail(anyString(), anyString(), anyString());

        // Обидва записи позначені як невдалі.
        verify(notificationService, times(2))
                .markAsFailed(any(UUID.class));

        verify(notificationService, never())
                .markAsSent(any(UUID.class));
    }

    @Test
    void notificationTypes_shouldMatchExpectedEvents() {
        notificationListener.onVotingStarted(
                new VotingStartedEvent(roomId)
        );

        notificationListener.onVotingResultsReady(
                new VotingResultsReadyEvent(roomId, List.of(11L), 2)
        );

        verify(notificationService).createNotification(
                eq(alice.getId()),
                eq(roomId),
                eq(NotificationType.VOTING_STARTED),
                anyString()
        );

        verify(notificationService).createNotification(
                eq(alice.getId()),
                eq(roomId),
                eq(NotificationType.RESULTS_READY),
                anyString()
        );
    }

    private User createUser(String name, String email) {
        User user = new User(name, email);

        ReflectionTestUtils.setField(
                user,
                "id",
                UUID.randomUUID()
        );

        return user;
    }
}
