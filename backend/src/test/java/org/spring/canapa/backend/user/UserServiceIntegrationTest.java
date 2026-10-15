package org.spring.canapa.backend.user;

import org.junit.jupiter.api.Test;
import org.spring.canapa.backend.user.dto.CreateUserCommand;
import org.spring.canapa.backend.user.dto.UpdateUserCommand;
import org.spring.canapa.backend.user.dto.UserData;
import org.spring.canapa.backend.user.exception.DuplicateUserEmailException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Test
    void createsRetrievesAndUpdatesUser() {
        UserData created = userService.createUser(
                new CreateUserCommand("  Alice  ", " Alice@Example.COM ")
        );

        UserData retrieved = userService.getUser(created.id());
        UserData updated = userService.updateUser(
                created.id(),
                new UpdateUserCommand("Alice Smith", "alice.smith@example.com")
        );

        assertThat(created.id()).isNotNull();
        assertThat(created.createdAt()).isNotNull();
        assertThat(retrieved).isEqualTo(created);
        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(updated.name()).isEqualTo("Alice Smith");
        assertThat(updated.email()).isEqualTo("alice.smith@example.com");
        assertThat(updated.createdAt()).isEqualTo(created.createdAt());
    }

    @Test
    void rejectsCaseInsensitiveDuplicateEmail() {
        userService.createUser(new CreateUserCommand("Alice", "alice@example.com"));

        assertThatThrownBy(() -> userService.createUser(
                new CreateUserCommand("Another Alice", "ALICE@EXAMPLE.COM")
        )).isInstanceOf(DuplicateUserEmailException.class);
    }
}
