package org.spring.canapa.backend.invitation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.spring.canapa.backend.room.Room;
import org.spring.canapa.backend.user.User;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;


@Getter
@Entity
@Table(name = "invitations")
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

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
            Room room,
            User createdBy,
            String code,
            Instant expiresAt
    ) {
        this.room = Objects.requireNonNull(room);
        this.createdBy = Objects.requireNonNull(createdBy);
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