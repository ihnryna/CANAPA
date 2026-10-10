package org.spring.canapa.backend.recommendation.dto;

import java.util.Objects;

public record RecommendedMovie(
        MovieCandidate movie,
        double score
) {
    public RecommendedMovie {
        Objects.requireNonNull(
                movie, "Movie is required"
        );

        if (!Double.isFinite(score)
                || score < 0.0
                || score > 1.0) {
            throw new IllegalArgumentException(
                    "Score must be between 0 and 1"
            );
        }
    }
}