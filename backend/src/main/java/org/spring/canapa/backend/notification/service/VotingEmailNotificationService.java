
package org.spring.canapa.backend.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spring.canapa.backend.movie.entity.Movie;
import org.spring.canapa.backend.movie.repository.MovieRepository;
import org.spring.canapa.backend.notification.entity.Notification;
import org.spring.canapa.backend.notification.entity.NotificationType;
import org.spring.canapa.backend.notification.event.VotingResultsReadyEvent;
import org.spring.canapa.backend.notification.event.VotingStartedEvent;
import org.spring.canapa.backend.room.Room;
import org.spring.canapa.backend.room.RoomRepository;
import org.spring.canapa.backend.voting.entity.VotingResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Service
public class VotingEmailNotificationService {

    private static final Logger log =
        LoggerFactory.getLogger(VotingEmailNotificationService.class);

    private final RoomRepository roomRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final MovieRepository movieRepository;

    public VotingEmailNotificationService(
        RoomRepository roomRepository,
        EmailService emailService,
        NotificationService notificationService,
        MovieRepository movieRepository
    ) {
        this.roomRepository = roomRepository;
        this.emailService = emailService;
        this.notificationService = notificationService;
        this.movieRepository = movieRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVotingStarted(VotingStartedEvent event) {
        try {
            Room room = roomRepository.findWithParticipantsById(event.roomId())
                .orElseThrow(() -> new IllegalStateException(
                    "Room not found: " + event.roomId()
                ));

            String message = "Голосування в кімнаті «" + room.getName()
                + "» розпочалося.";

            for (var participant : room.getParticipants()) {
                sendToParticipant(
                    participant.getId(),
                    participant.getEmail(),
                    room.getId(),
                    NotificationType.VOTING_STARTED,
                    message,
                    "CANAPA: голосування розпочалося",
                    "Привіт, " + participant.getName() + "!\n\n"
                        + message
                        + "\n\nЗаходь у CANAPA, щоб проголосувати."
                );
            }
        } catch (Exception exception) {
            log.error(
                "Could not process voting-started notifications for room {}",
                event.roomId(),
                exception
            );
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVotingResultsReady(VotingResultsReadyEvent event) {
        try {
            Room room = roomRepository.findWithParticipantsById(event.roomId())
                .orElseThrow(() -> new IllegalStateException(
                    "Room not found: " + event.roomId()
                ));

            List<String> winnerTitles = event.winnerMovieIds().stream()
                    .map(movieId -> movieRepository.findById(movieId)
                            .map(Movie::getTitle)
                            .orElse("Невідомий фільм"))
                    .toList();

            String winnersText;

            if (winnerTitles.isEmpty()) {
                winnersText =
                        "У голосуванні немає переможців: жоден фільм не отримав голосів.";
            } else if (winnerTitles.size() == 1) {
                winnersText = "Фільм-переможець: «"
                        + winnerTitles.getFirst() + "».";
            } else {
                winnersText = "Нічия! Фільми-переможці: "
                        + String.join(", ", winnerTitles) + ".";
            }

            String message = "Результати голосування в кімнаті «"
                + room.getName() + "». " + winnersText
                + " Кількість голосів за кожного переможця: "
                + event.voteCount() + ".";

            for (var participant : room.getParticipants()) {
                sendToParticipant(
                    participant.getId(),
                    participant.getEmail(),
                    room.getId(),
                    NotificationType.RESULTS_READY,
                    message,
                    "CANAPA: результати голосування",
                    "Привіт, " + participant.getName() + "!\n\n"
                        + message
                        + "\n\nДякуємо за участь у голосуванні!"
                );
            }
        } catch (Exception exception) {
            log.error(
                "Could not process voting-result notifications for room {}",
                event.roomId(),
                exception
            );
        }
    }

    private void sendToParticipant(
        java.util.UUID userId,
        String email,
        java.util.UUID roomId,
        NotificationType type,
        String notificationMessage,
        String subject,
        String emailBody
    ) {
        Notification notification;

        try {
            notification = notificationService.createNotification(
                userId,
                roomId,
                type,
                notificationMessage
            );
        } catch (Exception exception) {
            log.error(
                "Could not create notification for user {} in room {}",
                userId,
                roomId,
                exception
            );
            return;
        }

        try {
            emailService.sendTextEmail(email, subject, emailBody);
            notificationService.markAsSent(notification.getId());
        } catch (Exception exception) {
            log.warn(
                "Email notification {} failed for user {} in room {}",
                notification.getId(),
                userId,
                roomId,
                exception
            );

            try {
                notificationService.markAsFailed(notification.getId());
            } catch (Exception statusException) {
                log.error(
                    "Could not mark notification {} as FAILED",
                    notification.getId(),
                    statusException
                );
            }
        }
    }
}
