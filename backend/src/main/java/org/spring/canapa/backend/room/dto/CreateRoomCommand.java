package org.spring.canapa.backend.room.dto;

import java.util.UUID;

public record CreateRoomCommand(String name, UUID creatorId) {
}
