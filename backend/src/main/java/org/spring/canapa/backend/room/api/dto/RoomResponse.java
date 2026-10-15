package org.spring.canapa.backend.room.api.dto;

import org.spring.canapa.backend.room.RoomStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RoomResponse(
        UUID id,
        String name,
        RoomMemberResponse creator,
        List<RoomMemberResponse> participants,
        RoomStatus status,
        Instant createdAt
) {

    public RoomResponse {
        participants = List.copyOf(participants);
    }
}
