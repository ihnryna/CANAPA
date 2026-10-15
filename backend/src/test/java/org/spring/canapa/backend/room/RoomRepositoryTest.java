package org.spring.canapa.backend.room;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.spring.canapa.backend.user.User;
import org.spring.canapa.backend.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class RoomRepositoryTest {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void savesAndReadsRoomWithCreatorAndParticipants() {
        User creator = userRepository.save(new User("Alice", "alice@example.com"));
        User guest = userRepository.save(new User("Bob", "bob@example.com"));
        Room room = new Room("Friday movies", creator);
        room.addParticipant(guest);

        Room saved = roomRepository.saveAndFlush(room);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();

        entityManager.clear();

        Room found = roomRepository.findById(saved.getId()).orElseThrow();
        Set<String> participantEmails = found.getParticipants().stream()
                .map(User::getEmail)
                .collect(Collectors.toSet());

        assertThat(found.getName()).isEqualTo("Friday movies");
        assertThat(found.getCreator().getId()).isEqualTo(creator.getId());
        assertThat(found.getStatus()).isEqualTo(RoomStatus.CREATED);
        assertThat(participantEmails).containsExactlyInAnyOrder(
                "alice@example.com",
                "bob@example.com"
        );
    }

    @Test
    void rejectsBlankRoomNameAtDatabaseBoundary() {
        User creator = userRepository.save(new User("Alice", "alice@example.com"));

        assertThatThrownBy(() -> roomRepository.saveAndFlush(new Room("   ", creator)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsDuplicateMembership() {
        User creator = userRepository.save(new User("Alice", "alice@example.com"));
        Room room = roomRepository.saveAndFlush(new Room("Friday movies", creator));

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO room_participants (room_id, user_id) VALUES (?, ?)",
                room.getId(),
                creator.getId()
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRestrictsDeletingReferencedUser() {
        User creator = userRepository.save(new User("Alice", "alice@example.com"));
        roomRepository.saveAndFlush(new Room("Friday movies", creator));

        assertThatThrownBy(() -> jdbcTemplate.update(
                "DELETE FROM users WHERE id = ?",
                creator.getId()
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseDeletesMembershipRowsWithRoomButPreservesUsers() {
        User creator = userRepository.save(new User("Alice", "alice@example.com"));
        Room room = roomRepository.saveAndFlush(new Room("Friday movies", creator));

        jdbcTemplate.update("DELETE FROM rooms WHERE id = ?", room.getId());

        Integer membershipCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM room_participants WHERE room_id = ?",
                Integer.class,
                room.getId()
        );
        Integer userCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM users WHERE id = ?",
                Integer.class,
                creator.getId()
        );

        assertThat(membershipCount).isZero();
        assertThat(userCount).isOne();
    }
}
