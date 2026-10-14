package org.spring.canapa.backend.voting.dto;

import java.time.LocalDateTime;

public record VoteResponse(
        Long id,
        Long userId,
        Long roomId,
        Long movieId,
        LocalDateTime createdAt
) {
}