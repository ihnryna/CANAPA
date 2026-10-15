package org.spring.canapa.backend.room.exception;

import java.util.UUID;

public class RoomActionNotAllowedException extends RuntimeException {

    private final UUID roomId;
    private final UUID actorUserId;

    public RoomActionNotAllowedException(UUID roomId, UUID actorUserId) {
        super("User " + actorUserId + " is not allowed to change room " + roomId);
        this.roomId = roomId;
        this.actorUserId = actorUserId;
    }

    public UUID getRoomId() {
        return roomId;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }
}
