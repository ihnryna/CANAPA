package org.spring.canapa.backend.room.dto;

import java.util.UUID;

public record RoomMemberData(UUID id, String name, String email) {
}
