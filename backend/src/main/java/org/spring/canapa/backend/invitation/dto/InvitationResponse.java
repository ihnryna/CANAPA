package org.spring.canapa.backend.invitation.dto;

import org.spring.canapa.backend.invitation.entity.Invitation;
import org.spring.canapa.backend.invitation.entity.InvitationStatus;

import java.time.Instant;
import java.util.UUID;

public record InvitationResponse(
        UUID id,
        UUID roomId,
        UUID createdByUserId,
        String code,
        String link,
        Instant createdAt,
        Instant expiresAt,
        InvitationStatus status
) {
    public static InvitationResponse from(
            Invitation invitation,
            String link
    ) {
        return new InvitationResponse(
                invitation.getId(),
                invitation.getRoom().getId(),
                invitation.getCreatedBy().getId(),
                invitation.getCode(),
                link,
                invitation.getCreatedAt(),
                invitation.getExpiresAt(),
                invitation.getStatus()
        );
    }
}
