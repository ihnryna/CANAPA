package org.spring.canapa.backend.movie.dto;

import org.spring.canapa.backend.movie.entity.Movie;

import java.util.Set;

public record MovieResponse(
    Long id,
    Long tmdbId,
    String imdbId,
    String title,
    String description,
    Set<String> genres,
    Double rating,
    Integer runtime,
    Integer releaseYear,
    String posterUrl,
    String originalLanguage,
    String director,
    String actors,
    String writers,
    String rated
) {
    public static MovieResponse from(Movie movie) {
        return new MovieResponse(
            movie.getId(),
            movie.getTmdbId(),
            movie.getImdbId(),
            movie.getTitle(),
            movie.getDescription(),
            Set.copyOf(movie.getGenres()),
            movie.getRating(),
            movie.getRuntime(),
            movie.getReleaseYear(),
            movie.getPosterUrl(),
            movie.getOriginalLanguage(),
            movie.getDirector(),
            movie.getActors(),
            movie.getWriters(),
            movie.getRated()
        );
    }
}
