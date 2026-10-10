package org.spring.canapa.backend.recommendation.dto;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record MovieCandidate(
        UUID id,
        String title,
        String description,
        Set<String> genres,
        Double rating,
        Integer runtime,
        Integer releaseYear,
        String posterUrl
) {
    public MovieCandidate {
        Objects.requireNonNull(id, "Movie ID is required");

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                    "Movie title must not be blank"
            );
        }

        genres = genres == null
                ? Set.of()
                : Set.copyOf(genres);
    }
}