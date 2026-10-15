package org.spring.canapa.backend.movie.dto;

import java.util.List;

public record MovieSearchResult(
        int page,
        int totalPages,
        long totalResults,
        List<MovieSearchItem> results
) {
    public record MovieSearchItem(
            Long providerId,
            String title,
            String description,
            String posterUrl,
            String releaseDate,
            Double rating,
            String originalLanguage,
            List<String> genres
    ) {
    }
}
