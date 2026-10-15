package org.spring.canapa.backend.user.dto;

import java.time.Instant;
import java.util.UUID;

public record UserData(UUID id, String name, String email, Instant createdAt) {
}
