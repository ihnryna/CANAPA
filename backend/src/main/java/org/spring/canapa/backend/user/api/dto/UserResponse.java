package org.spring.canapa.backend.user.api.dto;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, String name, String email, Instant createdAt) {
}
