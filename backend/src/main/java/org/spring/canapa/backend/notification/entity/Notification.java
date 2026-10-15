package org.spring.canapa.backend.notification.entity;

import jakarta.persistence.*;
import lombok.Getter;

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

    // TEMPORARY: store IDs until Room and User modules are integrated.
    @Column(name = "recipient_user_id", nullable = false)
    private UUID recipientUserId;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

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
            UUID recipientUserId,
            UUID roomId,
            NotificationType type,
            String message
    ) {
        this.recipientUserId = Objects.requireNonNull(recipientUserId);
        this.roomId = Objects.requireNonNull(roomId);
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