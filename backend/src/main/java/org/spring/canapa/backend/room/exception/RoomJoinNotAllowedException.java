package org.spring.canapa.backend.room.exception;

import org.spring.canapa.backend.room.RoomStatus;

import java.util.UUID;

public class RoomJoinNotAllowedException extends RuntimeException {

    private final UUID roomId;
    private final RoomStatus status;

    public RoomJoinNotAllowedException(UUID roomId, RoomStatus status) {
        super("Cannot join room " + roomId + " while it is " + status);
        this.roomId = roomId;
        this.status = status;
    }

    public UUID getRoomId() {
        return roomId;
    }

    public RoomStatus getStatus() {
        return status;
    }
}
