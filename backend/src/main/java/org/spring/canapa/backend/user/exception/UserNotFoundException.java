package org.spring.canapa.backend.user.exception;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {

    private final UUID userId;

    public UserNotFoundException(UUID userId) {
        super("User not found: " + userId);
        this.userId = userId;
    }

    public UUID getUserId() {
        return userId;
    }
}
