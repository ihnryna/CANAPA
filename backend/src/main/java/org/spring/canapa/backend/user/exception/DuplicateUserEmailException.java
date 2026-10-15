package org.spring.canapa.backend.user.exception;

public class DuplicateUserEmailException extends RuntimeException {

    private final String email;

    public DuplicateUserEmailException(String email) {
        super("A user with email already exists: " + email);
        this.email = email;
    }

    public DuplicateUserEmailException(String email, Throwable cause) {
        super("A user with email already exists: " + email, cause);
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
