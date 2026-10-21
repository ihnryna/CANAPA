
package org.spring.canapa.backend.invitation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.spring.canapa.backend.invitation.entity.Invitation;
import org.spring.canapa.backend.invitation.entity.InvitationStatus;
import org.spring.canapa.backend.invitation.exception.InvitationExpiredException;
import org.spring.canapa.backend.invitation.exception.InvitationNotFoundException;
import org.spring.canapa.backend.invitation.repository.InvitationRepository;
import org.spring.canapa.backend.invitation.service.InvitationService;
import org.spring.canapa.backend.room.Room;
import org.spring.canapa.backend.room.RoomRepository;
import org.spring.canapa.backend.user.User;
import org.spring.canapa.backend.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@Testcontainers
class InvitationServiceRepositoryTest {

    @Container
    static final PostgreSQLContainer POSTGRES =
        new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("canapa_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name",
            () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private InvitationService invitationService;

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    private User user;
    private Room room;

    @BeforeEach
    void setUp() {
        invitationRepository.deleteAll();

        user = userRepository.save(
            new User("Invitation Test User", uniqueEmail())
        );

        room = roomRepository.save(
            new Room("Invitation Test Room", user)
        );
    }

    private String uniqueEmail() {
        return "invitation-" + UUID.randomUUID() + "@example.com";
    }

    @Test
    void shouldCreateInvitationWithUniqueCode() {
        Invitation first = invitationService.createInvitation(
            room.getId(), user.getId(), null
        );

        Invitation second = invitationService.createInvitation(
            room.getId(), user.getId(), null
        );

        assertNotNull(first.getId());
        assertNotNull(first.getCode());
        assertNotEquals(first.getCode(), second.getCode());
        assertEquals(InvitationStatus.ACTIVE, first.getStatus());
        assertTrue(first.getExpiresAt().isAfter(Instant.now()));
        assertEquals(room.getId(), first.getRoom().getId());
        assertEquals(user.getId(), first.getCreatedBy().getId());
    }

    @Test
    void shouldReturnValidInvitation() {
        Invitation created = invitationService.createInvitation(
            room.getId(), user.getId(), 24L
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
                room,
                user,
                "expired-test-code",
                Instant.now().plusMillis(500)
            )
        );

        try {
            Thread.sleep(600);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

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
