package org.spring.canapa.backend.voting.dto;

import java.util.UUID;

public record ExcludeMovieRequest(
        UUID userId,
        Long movieId
) {
}