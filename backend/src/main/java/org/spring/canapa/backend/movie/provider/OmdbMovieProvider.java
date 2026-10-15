package org.spring.canapa.backend.movie.provider;

import org.spring.canapa.backend.movie.config.MovieApiProperties;
import org.spring.canapa.backend.movie.dto.MovieSearchResult;
import org.spring.canapa.backend.movie.entity.Movie;
import org.spring.canapa.backend.movie.provider.exception.MovieProviderException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class OmdbMovieProvider implements MovieProvider {

    private final RestClient restClient;
    private final MovieApiProperties properties;

    public OmdbMovieProvider(
            RestClient movieRestClient,
            MovieApiProperties properties
    ) {
        this.restClient = movieRestClient;
        this.properties = properties;
    }

    @Override
    public String getProviderName() {
        return "OMDb";
    }

    @Override
    public boolean isConfigured() {
        return properties.getOmdb().getApiKey() != null
                && !properties.getOmdb().getApiKey().isBlank();
    }

    @Override
    public MovieSearchResult searchMovies(
            String query,
            int page,
            String language
    ) {
        requireConfigured();

        URI uri = UriComponentsBuilder
                .fromUriString(properties.getOmdb().getBaseUrl())
                .queryParam("apikey", properties.getOmdb().getApiKey())
                .queryParam("s", query)
                .queryParam("type", "movie")
                .queryParam("page", page)
                .build()
                .encode()
                .toUri();

        try {
            OmdbSearchResponse response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(OmdbSearchResponse.class);

            if (response == null || response.search() == null) {
                return new MovieSearchResult(1, 0, 0, List.of());
            }

            long total = parseLong(response.totalResults());

            List<MovieSearchResult.MovieSearchItem> results =
                    response.search().stream()
                            .map(item -> new MovieSearchResult.MovieSearchItem(
                                    null,
                                    item.title(),
                                    null,
                                    normalizePoster(item.poster()),
                                    item.year(),
                                    null,
                                    null,
                                    List.of()
                            ))
                            .toList();

            return new MovieSearchResult(
                    page,
                    (int) Math.ceil(total / 10.0),
                    total,
                    results
            );
        } catch (RestClientException ex) {
            throw new MovieProviderException("OMDb movie search failed", ex);
        }
    }

    @Override
    public Optional<Movie> getMovieDetails(Long providerMovieId) {
        return Optional.empty();
    }

    @Override
    public Optional<Movie> getMovieDetailsByImdbId(String imdbId) {
        requireConfigured();

        URI uri = UriComponentsBuilder
                .fromUriString(properties.getOmdb().getBaseUrl())
                .queryParam("apikey", properties.getOmdb().getApiKey())
                .queryParam("i", imdbId)
                .queryParam("plot", "full")
                .build()
                .encode()
                .toUri();

        try {
            OmdbDetailsResponse response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(OmdbDetailsResponse.class);

            if (response == null
                    || !"true".equalsIgnoreCase(response.response())) {
                return Optional.empty();
            }

            return Optional.of(toMovie(response));
        } catch (RestClientException ex) {
            throw new MovieProviderException("OMDb movie details request failed", ex);
        }
    }

    private Movie toMovie(OmdbDetailsResponse response) {
        Movie movie = new Movie();

        movie.setImdbId(response.imdbId());
        movie.setTitle(response.title() == null ? "Untitled" : response.title());
        movie.setDescription(response.plot());
        movie.setPosterUrl(normalizePoster(response.poster()));
        movie.setReleaseYear(parseYear(response.year()));
        movie.setRuntime(parseRuntime(response.runtime()));
        movie.setRating(parseRating(response.imdbRating()));
        movie.setDirector(response.director());
        movie.setActors(response.actors());
        movie.setWriters(response.writer());
        movie.setRated(response.rated());

        if (response.genre() != null && !response.genre().isBlank()) {
            Set<String> genres = Arrays.stream(response.genre().split(","))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .collect(Collectors.toSet());

            movie.setGenres(genres);
        }

        return movie;
    }

    private String normalizePoster(String poster) {
        return poster == null || poster.isBlank() || poster.equalsIgnoreCase("N/A")
                ? null
                : poster;
    }

    private Integer parseYear(String year) {
        if (year == null || year.length() < 4) {
            return null;
        }

        try {
            return Integer.parseInt(year.substring(0, 4));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer parseRuntime(String runtime) {
        if (runtime == null || runtime.isBlank()
                || runtime.equalsIgnoreCase("N/A")) {
            return null;
        }

        try {
            return Integer.parseInt(runtime.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Double parseRating(String rating) {
        if (rating == null || rating.isBlank()
                || rating.equalsIgnoreCase("N/A")) {
            return null;
        }

        try {
            return Double.parseDouble(rating);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private long parseLong(String value) {
        if (value == null) {
            return 0;
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private void requireConfigured() {
        if (!isConfigured()) {
            throw new MovieProviderException(
                    "OMDb is not configured. Set OMDB_API_KEY."
            );
        }
    }

    private record OmdbSearchResponse(
            List<OmdbSearchItem> search,
            String totalResults,
            String response
    ) {
        @com.fasterxml.jackson.annotation.JsonProperty("Search")
        public List<OmdbSearchItem> search() {
            return search;
        }

        @com.fasterxml.jackson.annotation.JsonProperty("totalResults")
        public String totalResults() {
            return totalResults;
        }

        @com.fasterxml.jackson.annotation.JsonProperty("Response")
        public String response() {
            return response;
        }
    }

    private record OmdbSearchItem(
            String title,
            String year,
            String imdbId,
            String poster
    ) {
        @com.fasterxml.jackson.annotation.JsonProperty("Title")
        public String title() {
            return title;
        }

        @com.fasterxml.jackson.annotation.JsonProperty("Year")
        public String year() {
            return year;
        }

        @com.fasterxml.jackson.annotation.JsonProperty("imdbID")
        public String imdbId() {
            return imdbId;
        }

        @com.fasterxml.jackson.annotation.JsonProperty("Poster")
        public String poster() {
            return poster;
        }
    }

    private record OmdbDetailsResponse(
            String title,
            String year,
            String rated,
            String runtime,
            String genre,
            String director,
            String writer,
            String actors,
            String plot,
            String poster,
            String imdbRating,
            String imdbId,
            String response
    ) {
        @com.fasterxml.jackson.annotation.JsonProperty("Title")
        public String title() {
            return title;
        }

        @com.fasterxml.jackson.annotation.JsonProperty("Year")
        public String year() {
            return year;
        }

        @com.fasterxml.jackson.annotation.JsonProperty("Rated")
        public String rated() {
            return rated;
        }

        @com.fasterxml.jackson.annotation.JsonProperty("Runtime")
        public String runtime() {
            return runtime;
        }

        @com.fasterxml.jackson.annotation.JsonProperty("Genre")
        public String genre() {
            return genre;
        }

        @com.fasterxml.jackson.annotation.JsonProperty("Director")
        public String director() {
            return director;
        }

        @com.fasterxml.jackson.annotation.JsonProperty("Writer")
        public String writer() {
            return writer;
        }

        @com.fasterxml.jackson.annotation.JsonProperty("Actors")
        public String actors() {
            return actors;
        }

        @com.fasterxml.jackson.annotation.JsonProperty("Plot")
        public String plot() {
            return plot;
        }

        @com.fasterxml.jackson.annotation.JsonProperty("Poster")
        public String poster() {
            return poster;
        }

        @com.fasterxml.jackson.annotation.JsonProperty("imdbRating")
        public String imdbRating() {
            return imdbRating;
        }

        @com.fasterxml.jackson.annotation.JsonProperty("imdbID")
        public String imdbId() {
            return imdbId;
        }

        @com.fasterxml.jackson.annotation.JsonProperty("Response")
        public String response() {
            return response;
        }
    }
}
