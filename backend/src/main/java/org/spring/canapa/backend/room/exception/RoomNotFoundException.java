package org.spring.canapa.backend.room.exception;

import java.util.UUID;

public class RoomNotFoundException extends RuntimeException {

    private final UUID roomId;

    public RoomNotFoundException(UUID roomId) {
        super("Room not found: " + roomId);
        this.roomId = roomId;
    }

    public UUID getRoomId() {
        return roomId;
    }
}
