package org.spring.canapa.backend.user.exception;

public class InvalidUserDataException extends RuntimeException {

    private final String field;

    public InvalidUserDataException(String field, String reason) {
        super("Invalid " + field + ": " + reason);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
