package org.spring.canapa.backend.voting.dto;

public record ExcludeMovieRequest(
        Long userId,
        Long movieId
) {
}