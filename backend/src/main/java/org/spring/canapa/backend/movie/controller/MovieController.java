package org.spring.canapa.backend.movie.controller;

import lombok.RequiredArgsConstructor;
import org.spring.canapa.backend.movie.dto.MovieResponse;
import org.spring.canapa.backend.movie.dto.MovieSearchResponse;
import org.spring.canapa.backend.movie.service.MovieService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/movies")
public class MovieController {

    private final MovieService movieService;

    @GetMapping("/search")
    public MovieSearchResponse searchMovies(
        @RequestParam(required = false) String query,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "en-US") String language
    ) {
        return movieService.searchMovies(query, page, size, language);
    }

    @GetMapping("/{id}")
    public MovieResponse getMovieById(@PathVariable Long id) {
        return movieService.getMovieById(id);
    }

    @GetMapping("/tmdb/{tmdbId}")
    public MovieResponse getMovieByTmdbId(@PathVariable Long tmdbId) {
        return movieService.getMovieByTmdbId(tmdbId);
    }

    @GetMapping("/imdb/{imdbId}")
    public MovieResponse getMovieByImdbId(@PathVariable String imdbId) {
        return movieService.getMovieByImdbId(imdbId);
    }

    @GetMapping("/popular")
    public List<MovieResponse> getPopularMovies(
        @RequestParam(defaultValue = "20") int limit
    ) {
        return movieService.getPopularLocalMovies(limit);
    }
}
