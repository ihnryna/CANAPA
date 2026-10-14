package org.spring.canapa.backend.invitation.service;

import org.spring.canapa.backend.invitation.entity.Invitation;
import org.spring.canapa.backend.invitation.entity.InvitationStatus;
import org.spring.canapa.backend.invitation.exception.InvitationExpiredException;
import org.spring.canapa.backend.invitation.exception.InvitationNotFoundException;
import org.spring.canapa.backend.invitation.repository.InvitationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class InvitationService {

    private static final int CODE_BYTES = 24;
    private static final int MAX_GENERATION_ATTEMPTS = 5;
    private static final long DEFAULT_EXPIRATION_HOURS = 24;

    private final InvitationRepository invitationRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String frontendBaseUrl;

    public InvitationService(
            InvitationRepository invitationRepository,
            @Value("${canapa.frontend.base-url:http://localhost:5173}")
            String frontendBaseUrl
    ) {
        this.invitationRepository = invitationRepository;
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
    }

    @Transactional
    public Invitation createInvitation(
            UUID roomId,
            UUID createdByUserId,
            Long expiresInHours
    ) {
        long hours = expiresInHours == null
                ? DEFAULT_EXPIRATION_HOURS
                : expiresInHours;

        if (hours <= 0) {
            throw new IllegalArgumentException(
                    "Invitation expiration must be greater than zero"
            );
        }

        String code = generateUniqueCode();
        Instant expiresAt = Instant.now().plusSeconds(hours * 3600);

        Invitation invitation = new Invitation(
                roomId,
                createdByUserId,
                code,
                expiresAt
        );

        return invitationRepository.save(invitation);
    }

    @Transactional(noRollbackFor = InvitationExpiredException.class)
    public Invitation getValidInvitation(String code) {
        Invitation invitation = invitationRepository.findByCode(code)
                .orElseThrow(() -> new InvitationNotFoundException(code));

        if (invitation.getStatus() == InvitationStatus.EXPIRED
                || invitation.isExpired(Instant.now())) {
            invitation.expire();
            invitationRepository.save(invitation);
            throw new InvitationExpiredException(code);
        }

        return invitation;
    }

    public String createInvitationLink(String code) {
        return frontendBaseUrl + "/join/" + code;
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            byte[] bytes = new byte[CODE_BYTES];
            secureRandom.nextBytes(bytes);

            String code = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(bytes);

            if (!invitationRepository.existsByCode(code)) {
                return code;
            }
        }

        throw new IllegalStateException(
                "Could not generate a unique invitation code"
        );
    }
}