package org.spring.canapa.backend.movie.provider;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.spring.canapa.backend.movie.config.MovieApiProperties;
import org.spring.canapa.backend.movie.dto.MovieSearchResult;
import org.spring.canapa.backend.movie.entity.Movie;
import org.spring.canapa.backend.movie.provider.exception.MovieProviderException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class TmdbMovieProvider implements MovieProvider {

    private static final String IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500";

    private final RestClient restClient;
    private final MovieApiProperties properties;

    public TmdbMovieProvider(
        RestClient movieRestClient,
        MovieApiProperties properties
    ) {
        this.restClient = movieRestClient;
        this.properties = properties;
    }

    @Override
    public String getProviderName() {
        return "TMDB";
    }

    @Override
    public boolean isConfigured() {
        return hasText(properties.getTmdb().getAccessToken())
            || hasText(properties.getTmdb().getApiKey());
    }

    @Override
    public MovieSearchResult searchMovies(
        String query,
        int page,
        String language
    ) {
        requireConfigured();

        URI uri = UriComponentsBuilder
            .fromUriString(properties.getTmdb().getBaseUrl())
            .path("/search/movie")
            .queryParam("query", query)
            .queryParam("page", page)
            .queryParam("include_adult", false)
            .queryParamIfPresent("language", Optional.ofNullable(language))
            .build()
            .encode()
            .toUri();

        try {
            TmdbSearchResponse response = restClient.get()
                .uri(addApiKeyIfNeeded(uri))
                .headers(headers -> {
                    if (hasText(properties.getTmdb().getAccessToken())) {
                        headers.setBearerAuth(
                            properties.getTmdb().getAccessToken()
                        );
                    }
                })
                .retrieve()
                .body(TmdbSearchResponse.class);

            if (response == null || response.results() == null) {
                return new MovieSearchResult(1, 0, 0, List.of());
            }

            List<MovieSearchResult.MovieSearchItem> results =
                response.results().stream()
                    .map(this::toSearchItem)
                    .toList();

            return new MovieSearchResult(
                response.page(),
                response.totalPages(),
                response.totalResults(),
                results
            );
        } catch (RestClientException ex) {
            throw new MovieProviderException(
                "TMDB movie search failed",
                ex
            );
        }
    }

    @Override
    public Optional<Movie> getMovieDetails(Long providerMovieId) {
        requireConfigured();

        URI uri = UriComponentsBuilder
            .fromUriString(properties.getTmdb().getBaseUrl())
            .path("/movie/{id}")
            .queryParam("append_to_response", "external_ids")
            .buildAndExpand(providerMovieId)
            .encode()
            .toUri();

        try {
            TmdbMovieResponse response = restClient.get()
                .uri(addApiKeyIfNeeded(uri))
                .headers(headers -> {
                    if (hasText(properties.getTmdb().getAccessToken())) {
                        headers.setBearerAuth(
                            properties.getTmdb().getAccessToken()
                        );
                    }
                })
                .retrieve()
                .body(TmdbMovieResponse.class);

            return Optional.ofNullable(response).map(this::toMovie);
        } catch (RestClientException ex) {
            throw new MovieProviderException(
                "TMDB movie details request failed",
                ex
            );
        }
    }

    @Override
    public Optional<Movie> getMovieDetailsByImdbId(String imdbId) {
        requireConfigured();

        URI uri = UriComponentsBuilder
            .fromUriString(properties.getTmdb().getBaseUrl())
            .path("/find/{id}")
            .queryParam("external_source", "imdb_id")
            .buildAndExpand(imdbId)
            .encode()
            .toUri();

        try {
            TmdbFindResponse response = restClient.get()
                .uri(addApiKeyIfNeeded(uri))
                .headers(headers -> {
                    if (hasText(properties.getTmdb().getAccessToken())) {
                        headers.setBearerAuth(
                            properties.getTmdb().getAccessToken()
                        );
                    }
                })
                .retrieve()
                .body(TmdbFindResponse.class);

            if (response == null || response.movieResults() == null) {
                return Optional.empty();
            }

            return response.movieResults().stream()
                .findFirst()
                .map(result -> getMovieDetails(result.id()))
                .orElse(Optional.empty());
        } catch (RestClientException ex) {
            throw new MovieProviderException(
                "TMDB IMDb lookup failed",
                ex
            );
        }
    }

    private URI addApiKeyIfNeeded(URI uri) {
        if (hasText(properties.getTmdb().getAccessToken())
            || !hasText(properties.getTmdb().getApiKey())) {
            return uri;
        }

        return UriComponentsBuilder.fromUri(uri)
            .queryParam("api_key", properties.getTmdb().getApiKey())
            .build()
            .encode()
            .toUri();
    }

    private MovieSearchResult.MovieSearchItem toSearchItem(
        TmdbMovieResponse response
    ) {
        return new MovieSearchResult.MovieSearchItem(
            response.id(),
            response.title(),
            response.overview(),
            posterUrl(response.posterPath()),
            response.releaseDate(),
            response.voteAverage(),
            response.originalLanguage(),
            List.of()
        );
    }

    private Movie toMovie(TmdbMovieResponse response) {
        Movie movie = new Movie();

        movie.setTmdbId(response.id());
        movie.setTitle(response.title() == null ? "Untitled" : response.title());
        movie.setDescription(response.overview());
        movie.setRating(response.voteAverage());
        movie.setRuntime(response.runtime());
        movie.setReleaseYear(parseYear(response.releaseDate()));
        movie.setPosterUrl(posterUrl(response.posterPath()));
        movie.setOriginalLanguage(response.originalLanguage());

        if (response.externalIds() != null) {
            movie.setImdbId(response.externalIds().imdbId());
        }

        if (response.genres() != null) {
            movie.setGenres(response.genres().stream()
                .map(TmdbGenre::name)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        }

        return movie;
    }

    private String posterUrl(String path) {
        return hasText(path) ? IMAGE_BASE_URL + path : null;
    }

    private Integer parseYear(String releaseDate) {
        if (releaseDate == null || releaseDate.length() < 4) {
            return null;
        }

        try {
            return Integer.parseInt(releaseDate.substring(0, 4));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void requireConfigured() {
        if (!isConfigured()) {
            throw new MovieProviderException(
                "TMDB is not configured. Set TMDB_ACCESS_TOKEN or TMDB_API_KEY."
            );
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record TmdbSearchResponse(
        @JsonProperty("page") int page,
        @JsonProperty("results") List<TmdbMovieResponse> results,
        @JsonProperty("total_pages") int totalPages,
        @JsonProperty("total_results") long totalResults
    ) {
    }

    private record TmdbFindResponse(
        @JsonProperty("movie_results")
        List<TmdbMovieResponse> movieResults
    ) {
    }

    private record TmdbMovieResponse(
        @JsonProperty("id") Long id,
        @JsonProperty("title") String title,
        @JsonProperty("overview") String overview,
        @JsonProperty("poster_path") String posterPath,
        @JsonProperty("release_date") String releaseDate,
        @JsonProperty("vote_average") Double voteAverage,
        @JsonProperty("runtime") Integer runtime,
        @JsonProperty("original_language") String originalLanguage,
        @JsonProperty("genres") List<TmdbGenre> genres,
        @JsonProperty("external_ids") TmdbExternalIds externalIds
    ) {
    }

    private record TmdbGenre(
        @JsonProperty("id") Long id,
        @JsonProperty("name") String name
    ) {
    }

    private record TmdbExternalIds(
        @JsonProperty("imdb_id") String imdbId
    ) {
    }
}
