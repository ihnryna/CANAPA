package org.spring.canapa.backend.voting.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ExcludedMovieResponse(
    Long id,
    UUID roomId,
    Long movieId,
    UUID excludedByUserId,
    LocalDateTime excludedAt
) {
}
