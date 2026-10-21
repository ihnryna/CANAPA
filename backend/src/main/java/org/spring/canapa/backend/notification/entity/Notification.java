package org.spring.canapa.backend.notification.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.spring.canapa.backend.room.Room;
import org.spring.canapa.backend.user.User;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private User recipient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    protected Notification() {
        // Required by JPA.
    }

    public Notification(
        User recipient,
        Room room,
        NotificationType type,
        String message
    ) {
        this.recipient = Objects.requireNonNull(recipient);
        this.room = Objects.requireNonNull(room);
        this.type = Objects.requireNonNull(type);
        this.message = Objects.requireNonNull(message);
        this.status = NotificationStatus.PENDING;
    }

    @PrePersist
    private void initializeCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void markAsSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = Instant.now();
    }

    public void markAsFailed() {
        this.status = NotificationStatus.FAILED;
        this.sentAt = null;
    }
}
