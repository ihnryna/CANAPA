package org.spring.canapa.backend.room.exception;

import org.spring.canapa.backend.room.RoomStatus;

import java.util.UUID;

public class InvalidRoomStatusTransitionException extends RuntimeException {

    private final UUID roomId;
    private final RoomStatus currentStatus;
    private final RoomStatus targetStatus;

    public InvalidRoomStatusTransitionException(
            UUID roomId,
            RoomStatus currentStatus,
            RoomStatus targetStatus
    ) {
        super("Cannot change room " + roomId + " status from " + currentStatus + " to " + targetStatus);
        this.roomId = roomId;
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }

    public UUID getRoomId() {
        return roomId;
    }

    public RoomStatus getCurrentStatus() {
        return currentStatus;
    }

    public RoomStatus getTargetStatus() {
        return targetStatus;
    }
}
