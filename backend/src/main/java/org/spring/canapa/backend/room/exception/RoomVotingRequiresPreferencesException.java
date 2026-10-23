package org.spring.canapa.backend.room.exception;

import java.util.UUID;

public class RoomVotingRequiresPreferencesException extends RuntimeException {

    private final UUID roomId;

    public RoomVotingRequiresPreferencesException(UUID roomId) {
        super("Cannot start voting in room " + roomId
            + " until at least one participant adds preferences");
        this.roomId = roomId;
    }

    public UUID getRoomId() {
        return roomId;
    }
}
