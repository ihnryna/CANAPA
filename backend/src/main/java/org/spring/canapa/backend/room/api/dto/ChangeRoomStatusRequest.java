package org.spring.canapa.backend.room.api.dto;

import org.spring.canapa.backend.room.RoomStatus;

public record ChangeRoomStatusRequest(RoomStatus status) {
}
