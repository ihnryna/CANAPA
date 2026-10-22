package org.spring.canapa.backend.invitation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CreateInvitationRequest(
    @Min(1)
    @Max(168)
    Long expiresInHours
) {
}
