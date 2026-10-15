package org.spring.canapa.backend.movie.provider;

import org.spring.canapa.backend.movie.dto.MovieSearchResult;
import org.spring.canapa.backend.movie.entity.Movie;

import java.util.Optional;

public interface MovieProvider {

    String getProviderName();

    MovieSearchResult searchMovies(String query, int page, String language);

    Optional<Movie> getMovieDetails(Long providerMovieId);

    Optional<Movie> getMovieDetailsByImdbId(String imdbId);

    boolean isConfigured();
}
