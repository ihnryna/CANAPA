package org.spring.canapa.backend.room.exception;

public class InvalidRoomDataException extends RuntimeException {

    private final String field;

    public InvalidRoomDataException(String field, String reason) {
        super("Invalid " + field + ": " + reason);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
