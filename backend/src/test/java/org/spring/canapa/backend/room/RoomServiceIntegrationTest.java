package org.spring.canapa.backend.room;

import org.junit.jupiter.api.Test;
import org.spring.canapa.backend.room.dto.CreateRoomCommand;
import org.spring.canapa.backend.room.dto.RoomData;
import org.spring.canapa.backend.room.exception.RoomJoinNotAllowedException;
import org.spring.canapa.backend.room.exception.RoomParticipantAlreadyExistsException;
import org.spring.canapa.backend.user.UserService;
import org.spring.canapa.backend.user.dto.CreateUserCommand;
import org.spring.canapa.backend.user.dto.UserData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Testcontainers
class RoomServiceIntegrationTest {

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
    private RoomService roomService;

    @Autowired
    private UserService userService;

    @Test
    void createsRetrievesJoinsAndAdvancesPersistedRoom() {
        UserData creator = userService.createUser(
            new CreateUserCommand("Alice", "alice.room.service@example.com")
        );
        UserData participant = userService.createUser(
            new CreateUserCommand("Bob", "bob.room.service@example.com")
        );

        RoomData created = roomService.createRoom(
            new CreateRoomCommand("  Friday movies  ", creator.id())
        );
        RoomData joined = roomService.joinRoom(created.id(), participant.id());
        RoomData voting = roomService.changeRoomStatus(
            created.id(), creator.id(), RoomStatus.VOTING
        );
        RoomData retrieved = roomService.getRoom(created.id());

        assertThat(created.name()).isEqualTo("Friday movies");
        assertThat(created.status()).isEqualTo(RoomStatus.CREATED);
        assertThat(created.participants()).extracting(member -> member.id())
            .containsExactly(creator.id());
        assertThat(joined.participants()).extracting(member -> member.id())
            .containsExactly(creator.id(), participant.id());
        assertThat(voting.status()).isEqualTo(RoomStatus.VOTING);
        assertThat(retrieved.status()).isEqualTo(RoomStatus.VOTING);
        assertThat(retrieved.participants()).extracting(member -> member.id())
            .containsExactly(creator.id(), participant.id());
    }

    @Test
    void enforcesDuplicateAndClosedRoomJoinRules() {
        UserData creator = userService.createUser(
            new CreateUserCommand("Alice", "alice.room.rules@example.com")
        );
        UserData participant = userService.createUser(
            new CreateUserCommand("Bob", "bob.room.rules@example.com")
        );
        RoomData room = roomService.createRoom(
            new CreateRoomCommand("Friday movies", creator.id())
        );

        assertThatThrownBy(() -> roomService.joinRoom(room.id(), creator.id()))
            .isInstanceOf(RoomParticipantAlreadyExistsException.class);

        roomService.changeRoomStatus(room.id(), creator.id(), RoomStatus.VOTING);

        assertThatThrownBy(() -> roomService.joinRoom(room.id(), participant.id()))
            .isInstanceOf(RoomJoinNotAllowedException.class);
    }
}
