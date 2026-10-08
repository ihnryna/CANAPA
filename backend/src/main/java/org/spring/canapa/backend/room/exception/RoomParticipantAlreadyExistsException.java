package org.spring.canapa.backend.room.exception;

import java.util.UUID;

public class RoomParticipantAlreadyExistsException extends RuntimeException {

    private final UUID roomId;
    private final UUID userId;

    public RoomParticipantAlreadyExistsException(UUID roomId, UUID userId) {
        super("User " + userId + " is already a participant of room " + roomId);
        this.roomId = roomId;
        this.userId = userId;
    }

    public RoomParticipantAlreadyExistsException(UUID roomId, UUID userId, Throwable cause) {
        super("User " + userId + " is already a participant of room " + roomId, cause);
        this.roomId = roomId;
        this.userId = userId;
    }

    public UUID getRoomId() {
        return roomId;
    }

    public UUID getUserId() {
        return userId;
    }
}
