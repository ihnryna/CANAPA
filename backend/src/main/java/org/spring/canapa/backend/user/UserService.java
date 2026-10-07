package org.spring.canapa.backend.user;

import org.spring.canapa.backend.user.dto.CreateUserCommand;
import org.spring.canapa.backend.user.dto.UpdateUserCommand;
import org.spring.canapa.backend.user.dto.UserData;
import org.spring.canapa.backend.user.exception.DuplicateUserEmailException;
import org.spring.canapa.backend.user.exception.InvalidUserDataException;
import org.spring.canapa.backend.user.exception.UserNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class UserService {

    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_EMAIL_LENGTH = 320;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserData createUser(CreateUserCommand command) {
        if (command == null) {
            throw new InvalidUserDataException("request", "must not be null");
        }

        String name = normalizeAndValidateName(command.name());
        String email = normalizeAndValidateEmail(command.email());
        ensureEmailAvailable(email, null);

        try {
            return toData(userRepository.saveAndFlush(new User(name, email)));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateUserEmailException(email, exception);
        }
    }

    @Transactional(readOnly = true)
    public UserData getUser(UUID userId) {
        return toData(findUser(userId));
    }

    @Transactional
    public UserData updateUser(UUID userId, UpdateUserCommand command) {
        if (command == null) {
            throw new InvalidUserDataException("request", "must not be null");
        }

        User user = findUser(userId);
        String name = normalizeAndValidateName(command.name());
        String email = normalizeAndValidateEmail(command.email());
        ensureEmailAvailable(email, user.getId());
        user.updateProfile(name, email);

        try {
            return toData(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateUserEmailException(email, exception);
        }
    }

    private User findUser(UUID userId) {
        if (userId == null) {
            throw new InvalidUserDataException("userId", "must not be null");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private void ensureEmailAvailable(String email, UUID currentUserId) {
        userRepository.findByEmailIgnoreCase(email)
                .filter(existingUser -> !existingUser.getId().equals(currentUserId))
                .ifPresent(existingUser -> {
                    throw new DuplicateUserEmailException(email);
                });
    }

    private String normalizeAndValidateName(String value) {
        if (value == null) {
            throw new InvalidUserDataException("name", "must not be null");
        }

        String normalized = value.strip();
        if (normalized.isEmpty()) {
            throw new InvalidUserDataException("name", "must not be blank");
        }
        if (normalized.length() > MAX_NAME_LENGTH) {
            throw new InvalidUserDataException("name", "must not exceed 100 characters");
        }
        return normalized;
    }

    private String normalizeAndValidateEmail(String value) {
        if (value == null) {
            throw new InvalidUserDataException("email", "must not be null");
        }

        String normalized = value.strip().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new InvalidUserDataException("email", "must not be blank");
        }
        if (normalized.length() > MAX_EMAIL_LENGTH) {
            throw new InvalidUserDataException("email", "must not exceed 320 characters");
        }
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new InvalidUserDataException("email", "must be a valid email address");
        }
        return normalized;
    }

    private UserData toData(User user) {
        return new UserData(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCreatedAt()
        );
    }
}
