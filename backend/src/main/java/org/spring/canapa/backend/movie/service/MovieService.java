package org.spring.canapa.backend.movie.service;

import org.spring.canapa.backend.movie.dto.MovieResponse;
import org.spring.canapa.backend.movie.dto.MovieSearchResponse;
import org.spring.canapa.backend.movie.dto.MovieSearchResult;
import org.spring.canapa.backend.movie.entity.Movie;
import org.spring.canapa.backend.movie.provider.OmdbMovieProvider;
import org.spring.canapa.backend.movie.provider.TmdbMovieProvider;
import org.spring.canapa.backend.movie.provider.exception.MovieProviderException;
import org.spring.canapa.backend.movie.repository.MovieRepository;
import org.spring.canapa.backend.movie.service.exception.MovieNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class MovieService {

    private final MovieRepository movieRepository;
    private final TmdbMovieProvider tmdbProvider;
    private final OmdbMovieProvider omdbProvider;

    public MovieService(
            MovieRepository movieRepository,
            TmdbMovieProvider tmdbProvider,
            OmdbMovieProvider omdbProvider
    ) {
        this.movieRepository = movieRepository;
        this.tmdbProvider = tmdbProvider;
        this.omdbProvider = omdbProvider;
    }

    @Transactional(readOnly = true)
    public MovieSearchResponse searchMovies(
            String query,
            int page,
            int size,
            String language
    ) {
        validatePagination(page, size);

        if (!StringUtils.hasText(query)) {
            var results = movieRepository.findAll(
                    PageRequest.of(page - 1, size, Sort.by("title").ascending())
            );

            return new MovieSearchResponse(
                    page,
                    results.getTotalPages(),
                    results.getTotalElements(),
                    results.getContent().stream().map(MovieResponse::from).toList()
            );
        }

        String normalizedQuery = query.trim();

        if (tmdbProvider.isConfigured()) {
            MovieSearchResult result = tmdbProvider.searchMovies(
                    normalizedQuery,
                    page,
                    normalizeLanguage(language)
            );

            List<MovieResponse> movies = result.results().stream()
                    .map(this::saveSearchResult)
                    .map(MovieResponse::from)
                    .toList();

            return new MovieSearchResponse(
                    result.page(),
                    result.totalPages(),
                    result.totalResults(),
                    movies
            );
        }

        if (omdbProvider.isConfigured()) {
            MovieSearchResult result = omdbProvider.searchMovies(
                    normalizedQuery,
                    page,
                    language
            );

            List<MovieResponse> movies = result.results().stream()
                    .map(this::saveOmdbSearchResult)
                    .map(MovieResponse::from)
                    .toList();

            return new MovieSearchResponse(
                    result.page(),
                    result.totalPages(),
                    result.totalResults(),
                    movies
            );
        }

        var localResults = movieRepository.findByTitleContainingIgnoreCase(
                normalizedQuery,
                PageRequest.of(page - 1, size, Sort.by("title").ascending())
        );

        return new MovieSearchResponse(
                page,
                localResults.getTotalPages(),
                localResults.getTotalElements(),
                localResults.getContent().stream().map(MovieResponse::from).toList()
        );
    }

    @Transactional
    public MovieResponse getMovieById(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new MovieNotFoundException(
                        "Movie not found: " + id
                ));

        return MovieResponse.from(enrichMovie(movie));
    }

    @Transactional
    public MovieResponse getMovieByTmdbId(Long tmdbId) {
        Optional<Movie> existing = movieRepository.findByTmdbId(tmdbId);

        if (existing.isPresent()) {
            return MovieResponse.from(enrichMovie(existing.get()));
        }

        if (!tmdbProvider.isConfigured()) {
            throw new MovieProviderException("TMDB is not configured");
        }

        Movie movie = tmdbProvider.getMovieDetails(tmdbId)
                .orElseThrow(() -> new MovieNotFoundException(
                        "Movie not found in TMDB: " + tmdbId
                ));

        return MovieResponse.from(saveMovie(movie));
    }

    @Transactional
    public MovieResponse getMovieByImdbId(String imdbId) {
        if (!StringUtils.hasText(imdbId)) {
            throw new IllegalArgumentException("IMDb ID must not be blank");
        }

        String normalizedId = imdbId.trim();

        Optional<Movie> existing = movieRepository.findByImdbId(normalizedId);

        if (existing.isPresent()) {
            return MovieResponse.from(enrichMovie(existing.get()));
        }

        Optional<Movie> movie = Optional.empty();

        if (omdbProvider.isConfigured()) {
            movie = omdbProvider.getMovieDetailsByImdbId(normalizedId);
        }

        if (movie.isEmpty() && tmdbProvider.isConfigured()) {
            movie = tmdbProvider.getMovieDetailsByImdbId(normalizedId);
        }

        Movie result = movie.orElseThrow(() -> new MovieNotFoundException(
                "Movie not found: " + normalizedId
        ));

        return MovieResponse.from(saveMovie(result));
    }

    @Transactional(readOnly = true)
    public List<MovieResponse> getPopularLocalMovies(int limit) {
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("Limit must be between 1 and 100");
        }

        return movieRepository.findAll(
                PageRequest.of(
                        0,
                        limit,
                        Sort.by(Sort.Direction.DESC, "rating")
                                .and(Sort.by(Sort.Direction.ASC, "title"))
                )
        ).getContent().stream().map(MovieResponse::from).toList();
    }

    private Movie saveSearchResult(MovieSearchResult.MovieSearchItem item) {
        if (item.providerId() == null) {
            return saveOmdbSearchResult(item);
        }

        Movie movie = movieRepository.findByTmdbId(item.providerId())
                .orElseGet(Movie::new);

        movie.setTmdbId(item.providerId());
        movie.setTitle(item.title() == null ? "Untitled" : item.title());
        movie.setDescription(item.description());
        movie.setPosterUrl(item.posterUrl());
        movie.setRating(item.rating());
        movie.setOriginalLanguage(item.originalLanguage());

        if (item.releaseDate() != null && item.releaseDate().length() >= 4) {
            try {
                movie.setReleaseYear(
                        Integer.parseInt(item.releaseDate().substring(0, 4))
                );
            } catch (NumberFormatException ignored) {
                movie.setReleaseYear(null);
            }
        }

        if (item.genres() != null && !item.genres().isEmpty()) {
            movie.setGenres(new java.util.HashSet<>(item.genres()));
        }

        return saveMovie(movie);
    }

    private Movie saveOmdbSearchResult(MovieSearchResult.MovieSearchItem item) {
        Movie movie = new Movie();

        movie.setTitle(item.title() == null ? "Untitled" : item.title());
        movie.setDescription(item.description());
        movie.setPosterUrl(item.posterUrl());
        movie.setRating(item.rating());
        movie.setOriginalLanguage(item.originalLanguage());

        if (item.releaseDate() != null && item.releaseDate().length() >= 4) {
            try {
                movie.setReleaseYear(
                        Integer.parseInt(item.releaseDate().substring(0, 4))
                );
            } catch (NumberFormatException ignored) {
                movie.setReleaseYear(null);
            }
        }

        return saveMovie(movie);
    }

    private Movie enrichMovie(Movie movie) {
        if (movie.getTmdbId() != null && tmdbProvider.isConfigured()) {
            try {
                Movie details = tmdbProvider.getMovieDetails(movie.getTmdbId())
                        .orElse(null);

                if (details != null) {
                    mergeDetails(movie, details);
                    movie = movieRepository.save(movie);
                }
            } catch (MovieProviderException ignored) {
                // Return cached details when an external provider is unavailable.
            }
        }

        if (movie.getImdbId() != null && omdbProvider.isConfigured()
                && (movie.getDirector() == null || movie.getActors() == null)) {
            try {
                Movie details = omdbProvider
                        .getMovieDetailsByImdbId(movie.getImdbId())
                        .orElse(null);

                if (details != null) {
                    mergeDetails(movie, details);
                    movie = movieRepository.save(movie);
                }
            } catch (MovieProviderException ignored) {
                // Return cached details when an external provider is unavailable.
            }
        }

        return movie;
    }

    private void mergeDetails(Movie target, Movie source) {
        if (target.getTmdbId() == null) target.setTmdbId(source.getTmdbId());
        if (target.getImdbId() == null) target.setImdbId(source.getImdbId());
        if (!StringUtils.hasText(target.getDescription())) {
            target.setDescription(source.getDescription());
        }
        if (target.getRating() == null) target.setRating(source.getRating());
        if (target.getRuntime() == null) target.setRuntime(source.getRuntime());
        if (target.getReleaseYear() == null) {
            target.setReleaseYear(source.getReleaseYear());
        }
        if (!StringUtils.hasText(target.getPosterUrl())) {
            target.setPosterUrl(source.getPosterUrl());
        }
        if (!StringUtils.hasText(target.getOriginalLanguage())) {
            target.setOriginalLanguage(source.getOriginalLanguage());
        }
        if (!StringUtils.hasText(target.getDirector())) {
            target.setDirector(source.getDirector());
        }
        if (!StringUtils.hasText(target.getActors())) {
            target.setActors(source.getActors());
        }
        if (!StringUtils.hasText(target.getWriters())) {
            target.setWriters(source.getWriters());
        }
        if (!StringUtils.hasText(target.getRated())) {
            target.setRated(source.getRated());
        }
        if (target.getGenres().isEmpty() && !source.getGenres().isEmpty()) {
            target.setGenres(source.getGenres());
        }
    }

    private Movie saveMovie(Movie movie) {
        if (movie.getTmdbId() != null) {
            Optional<Movie> existing = movieRepository.findByTmdbId(
                    movie.getTmdbId()
            );

            if (existing.isPresent()) {
                Movie target = existing.get();
                mergeDetails(target, movie);
                return movieRepository.save(target);
            }
        }

        if (StringUtils.hasText(movie.getImdbId())) {
            Optional<Movie> existing = movieRepository.findByImdbId(
                    movie.getImdbId()
            );

            if (existing.isPresent()) {
                Movie target = existing.get();
                mergeDetails(target, movie);
                return movieRepository.save(target);
            }
        }

        return movieRepository.save(movie);
    }

    private String normalizeLanguage(String language) {
        if (!StringUtils.hasText(language)) {
            return "en-US";
        }

        return language.trim().toLowerCase(Locale.ROOT).contains("-")
                ? language.trim()
                : language.trim() + "-US";
    }

    private void validatePagination(int page, int size) {
        if (page < 1) {
            throw new IllegalArgumentException("Page must be at least 1");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Size must be between 1 and 100");
        }
    }
}
