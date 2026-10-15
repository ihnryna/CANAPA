package org.spring.canapa.backend.movie.dto;

import java.util.List;

public record MovieSearchResponse(
        int page,
        int totalPages,
        long totalResults,
        List<MovieResponse> results
) {
}
