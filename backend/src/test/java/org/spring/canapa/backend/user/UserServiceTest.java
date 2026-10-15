package org.spring.canapa.backend.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spring.canapa.backend.user.dto.CreateUserCommand;
import org.spring.canapa.backend.user.dto.UpdateUserCommand;
import org.spring.canapa.backend.user.dto.UserData;
import org.spring.canapa.backend.user.exception.DuplicateUserEmailException;
import org.spring.canapa.backend.user.exception.InvalidUserDataException;
import org.spring.canapa.backend.user.exception.UserNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void createsUserWithNormalizedData() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findByEmailIgnoreCase("alice@example.com"))
                .thenReturn(Optional.empty());
        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> persisted(invocation.getArgument(0), userId));

        UserData result = userService.createUser(
                new CreateUserCommand("  Alice  Smith  ", "  Alice@Example.COM ")
        );

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.name()).isEqualTo("Alice  Smith");
        assertThat(result.email()).isEqualTo("alice@example.com");
        assertThat(result.createdAt()).isNotNull();
        verify(userRepository).findByEmailIgnoreCase("alice@example.com");
        verify(userRepository).saveAndFlush(savedUser.capture());
        assertThat(savedUser.getValue().getName()).isEqualTo("Alice  Smith");
        assertThat(savedUser.getValue().getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void rejectsNullCreateCommand() {
        assertThatThrownBy(() -> userService.createUser(null))
                .isInstanceOf(InvalidUserDataException.class)
                .hasMessage("Invalid request: must not be null");

        verifyNoInteractions(userRepository);
    }

    @ParameterizedTest(name = "[{index}] name={0}")
    @MethodSource("invalidNameCases")
    void rejectsInvalidName(String name, String expectedReason) {
        assertThatThrownBy(() -> userService.createUser(
                new CreateUserCommand(name, "alice@example.com")
        )).isInstanceOf(InvalidUserDataException.class)
                .hasMessage("Invalid name: " + expectedReason);

        verifyNoInteractions(userRepository);
    }

    @ParameterizedTest(name = "[{index}] email={0}")
    @MethodSource("invalidEmailCases")
    void rejectsInvalidEmail(String email, String expectedReason) {
        assertThatThrownBy(() -> userService.createUser(
                new CreateUserCommand("Alice", email)
        )).isInstanceOf(InvalidUserDataException.class)
                .hasMessage("Invalid email: " + expectedReason);

        verifyNoInteractions(userRepository);
    }

    @Test
    void acceptsMaximumNameAndEmailLengths() {
        String maximumName = "n".repeat(100);
        String maximumEmail = "a".repeat(308) + "@example.com";
        UUID userId = UUID.randomUUID();
        when(userRepository.findByEmailIgnoreCase(maximumEmail))
                .thenReturn(Optional.empty());
        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> persisted(invocation.getArgument(0), userId));

        UserData result = userService.createUser(
                new CreateUserCommand(maximumName, maximumEmail)
        );

        assertThat(result.name()).hasSize(100);
        assertThat(result.email()).hasSize(320);
    }

    @Test
    void rejectsExistingEmailBeforeCreate() {
        User existing = persisted(new User("Existing", "alice@example.com"), UUID.randomUUID());
        when(userRepository.findByEmailIgnoreCase("alice@example.com"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> userService.createUser(
                new CreateUserCommand("Alice", "ALICE@example.com")
        )).isInstanceOf(DuplicateUserEmailException.class);

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void translatesConcurrentEmailConflict() {
        when(userRepository.findByEmailIgnoreCase("alice@example.com"))
                .thenReturn(Optional.empty());
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("unique index"));

        assertThatThrownBy(() -> userService.createUser(
                new CreateUserCommand("Alice", "alice@example.com")
        )).isInstanceOf(DuplicateUserEmailException.class)
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
    }

    private static Stream<Arguments> invalidNameCases() {
        return Stream.of(
                Arguments.of((String) null, "must not be null"),
                Arguments.of("", "must not be blank"),
                Arguments.of(" \t\n ", "must not be blank"),
                Arguments.of("n".repeat(101), "must not exceed 100 characters")
        );
    }

    private static Stream<Arguments> invalidEmailCases() {
        String tooLongEmail = "a".repeat(309) + "@example.com";
        return Stream.of(
                Arguments.of((String) null, "must not be null"),
                Arguments.of("", "must not be blank"),
                Arguments.of(" \t\n ", "must not be blank"),
                Arguments.of(tooLongEmail, "must not exceed 320 characters"),
                Arguments.of("alice.example.com", "must be a valid email address"),
                Arguments.of("@example.com", "must be a valid email address"),
                Arguments.of("alice@", "must be a valid email address"),
                Arguments.of("alice@example", "must be a valid email address"),
                Arguments.of("alice@@example.com", "must be a valid email address"),
                Arguments.of("alice @example.com", "must be a valid email address"),
                Arguments.of("alice@exa mple.com", "must be a valid email address")
        );
    }

    @Test
    void retrievesExistingUser() {
        UUID userId = UUID.randomUUID();
        User user = persisted(new User("Alice", "alice@example.com"), userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserData result = userService.getUser(userId);

        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.name()).isEqualTo("Alice");
        assertThat(result.email()).isEqualTo("alice@example.com");
    }

    @Test
    void failsWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser(userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found: " + userId);
    }

    @Test
    void updatesAndNormalizesUserProfile() {
        UUID userId = UUID.randomUUID();
        User user = persisted(new User("Alice", "alice@example.com"), userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.findByEmailIgnoreCase("alice.new@example.com"))
                .thenReturn(Optional.empty());
        when(userRepository.saveAndFlush(user)).thenReturn(user);

        UserData result = userService.updateUser(
                userId,
                new UpdateUserCommand("  Alice Smith ", " Alice.New@Example.com ")
        );

        assertThat(result.name()).isEqualTo("Alice Smith");
        assertThat(result.email()).isEqualTo("alice.new@example.com");
        verify(userRepository).saveAndFlush(user);
    }

    @Test
    void allowsUserToKeepOwnEmail() {
        UUID userId = UUID.randomUUID();
        User user = persisted(new User("Alice", "alice@example.com"), userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.findByEmailIgnoreCase("alice@example.com"))
                .thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(user)).thenReturn(user);

        UserData result = userService.updateUser(
                userId,
                new UpdateUserCommand("Alice Smith", "ALICE@EXAMPLE.COM")
        );

        assertThat(result.email()).isEqualTo("alice@example.com");
    }

    @Test
    void rejectsEmailOwnedByAnotherUserDuringUpdate() {
        UUID userId = UUID.randomUUID();
        User user = persisted(new User("Alice", "alice@example.com"), userId);
        User other = persisted(new User("Bob", "bob@example.com"), UUID.randomUUID());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.findByEmailIgnoreCase("bob@example.com"))
                .thenReturn(Optional.of(other));

        assertThatThrownBy(() -> userService.updateUser(
                userId,
                new UpdateUserCommand("Alice", "BOB@example.com")
        )).isInstanceOf(DuplicateUserEmailException.class);

        verify(userRepository, never()).saveAndFlush(user);
    }

    private User persisted(User user, UUID id) {
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "createdAt", Instant.parse("2026-10-07T12:00:00Z"));
        return user;
    }
}
