package org.spring.canapa.backend.invitation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.spring.canapa.backend.invitation.entity.Invitation;
import org.spring.canapa.backend.invitation.entity.InvitationStatus;
import org.spring.canapa.backend.invitation.exception.InvitationExpiredException;
import org.spring.canapa.backend.invitation.exception.InvitationNotFoundException;
import org.spring.canapa.backend.invitation.repository.InvitationRepository;
import org.spring.canapa.backend.invitation.service.InvitationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class InvitationServiceRepositoryTest {

    @Autowired
    private InvitationService invitationService;

    @Autowired
    private InvitationRepository invitationRepository;

    @BeforeEach
    void setUp() {
        invitationRepository.deleteAll();
    }

    @Test
    void shouldCreateInvitationWithUniqueCode() {
        UUID roomId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Invitation first = invitationService.createInvitation(
                roomId, userId, null
        );
        Invitation second = invitationService.createInvitation(
                roomId, userId, null
        );

        assertNotNull(first.getId());
        assertNotNull(first.getCode());
        assertNotEquals(first.getCode(), second.getCode());
        assertEquals(InvitationStatus.ACTIVE, first.getStatus());
        assertTrue(first.getExpiresAt().isAfter(Instant.now()));
        assertEquals(roomId, first.getRoomId());
        assertEquals(userId, first.getCreatedByUserId());
    }

    @Test
    void shouldReturnValidInvitation() {
        Invitation created = invitationService.createInvitation(
                UUID.randomUUID(),
                UUID.randomUUID(),
                24L
        );

        Invitation found = invitationService.getValidInvitation(
                created.getCode()
        );

        assertEquals(created.getId(), found.getId());
        assertEquals(InvitationStatus.ACTIVE, found.getStatus());
    }

    @Test
    void shouldThrowWhenInvitationCodeDoesNotExist() {
        assertThrows(
                InvitationNotFoundException.class,
                () -> invitationService.getValidInvitation("invalid-code")
        );
    }

    @Test
    void shouldRejectExpiredInvitationAndPersistExpiredStatus() {
        Invitation expired = invitationRepository.save(
                new Invitation(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "expired-test-code",
                        Instant.now().minusSeconds(60)
                )
        );

        assertThrows(
                InvitationExpiredException.class,
                () -> invitationService.getValidInvitation(expired.getCode())
        );

        Invitation updated = invitationRepository
                .findByCode(expired.getCode())
                .orElseThrow();

        assertEquals(InvitationStatus.EXPIRED, updated.getStatus());
    }

    @Test
    void shouldBuildInvitationLink() {
        String link = invitationService.createInvitationLink("test-code");

        assertEquals(
                "http://localhost:5173/join/test-code",
                link
        );
    }
}