package org.spring.canapa.backend.invitation.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/*
 * TEMPORARY: roomId and createdByUserId are stored as UUIDs instead of
 * Room and User relationships until the room and user modules are merged.
 * Replace these fields with entity relationships after integration.
 */
@Getter
@Entity
@Table(name = "invitations")
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvitationStatus status;

    protected Invitation() {}

    public Invitation(
            UUID roomId,
            UUID createdByUserId,
            String code,
            Instant expiresAt
    ) {
        this.roomId = Objects.requireNonNull(roomId);
        this.createdByUserId = Objects.requireNonNull(createdByUserId);
        this.code = Objects.requireNonNull(code);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        this.status = InvitationStatus.ACTIVE;
    }

    @PrePersist
    private void initializeCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public void expire() {
        this.status = InvitationStatus.EXPIRED;
    }
}