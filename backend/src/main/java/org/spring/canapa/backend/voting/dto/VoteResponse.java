package org.spring.canapa.backend.voting.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record VoteResponse(
        Long id,
        UUID userId,
        UUID roomId,
        Long movieId,
        LocalDateTime createdAt
) {
}