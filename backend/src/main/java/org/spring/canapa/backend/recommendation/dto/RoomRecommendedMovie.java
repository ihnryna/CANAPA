package org.spring.canapa.backend.recommendation.dto;

import org.spring.canapa.backend.movie.dto.MovieResponse;

public record RoomRecommendedMovie(
        MovieResponse movie,
        double score
) {
}
