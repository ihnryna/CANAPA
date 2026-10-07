package org.spring.canapa.backend.user;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void savesAndReadsUser() {
        User saved = userRepository.saveAndFlush(new User("Alice", "alice@example.com"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();

        entityManager.clear();

        User found = userRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getName()).isEqualTo("Alice");
        assertThat(found.getEmail()).isEqualTo("alice@example.com");
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    void rejectsEmailThatDiffersOnlyByCase() {
        userRepository.saveAndFlush(new User("Alice", "alice@example.com"));

        assertThatThrownBy(() ->
                userRepository.saveAndFlush(new User("Another Alice", "ALICE@example.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsBlankNameAtDatabaseBoundary() {
        assertThatThrownBy(() ->
                userRepository.saveAndFlush(new User("   ", "alice@example.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
