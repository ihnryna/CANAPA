package org.spring.canapa.backend.room.dto;

import org.spring.canapa.backend.room.RoomStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RoomData(
        UUID id,
        String name,
        RoomMemberData creator,
        List<RoomMemberData> participants,
        RoomStatus status,
        Instant createdAt
) {

    public RoomData {
        participants = List.copyOf(participants);
    }
}
