package org.spring.canapa.backend.voting.dto;

import java.time.LocalDateTime;

public record ExcludedMovieResponse(
        Long id,
        Long roomId,
        Long movieId,
        Long excludedByUserId,
        LocalDateTime excludedAt
) {
}