package org.spring.canapa.backend.movie.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.spring.canapa.backend.movie.config.MovieApiProperties;
import org.spring.canapa.backend.movie.dto.MovieSearchResult;
import org.spring.canapa.backend.movie.entity.Movie;
import org.spring.canapa.backend.movie.provider.exception.MovieProviderException;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OmdbMovieProviderTest {

    private MovieApiProperties properties;
    private MockRestServiceServer server;
    private OmdbMovieProvider provider;

    @BeforeEach
    void setUp() {
        properties = new MovieApiProperties();
        properties.getOmdb().setBaseUrl("https://www.omdbapi.com/");
        properties.getOmdb().setApiKey("test-key");

        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();

        provider = new OmdbMovieProvider(
            builder.build(),
            properties
        );
    }

    @Test
    void getProviderNameReturnsOmdb() {
        assertEquals("OMDb", provider.getProviderName());
    }

    @Test
    void isConfiguredReturnsTrueWhenApiKeyExists() {
        assertTrue(provider.isConfigured());
    }

    @Test
    void isConfiguredReturnsFalseWhenApiKeyIsMissing() {
        properties.getOmdb().setApiKey("");

        assertFalse(provider.isConfigured());
    }

    @Test
    void searchMoviesReturnsResults() {
        server.expect(requestTo(
                org.hamcrest.Matchers.allOf(
                    org.hamcrest.Matchers.containsString("apikey=test-key"),
                    org.hamcrest.Matchers.containsString("s=Inception"),
                    org.hamcrest.Matchers.containsString("type=movie"),
                    org.hamcrest.Matchers.containsString("page=1")
                )
            ))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("""
                {
                  "Search": [
                    {
                      "Title": "Inception",
                      "Year": "2010",
                      "imdbID": "tt1375666",
                      "Type": "movie",
                      "Poster": "https://example.com/inception.jpg"
                    },
                    {
                      "Title": "Interstellar",
                      "Year": "2014",
                      "imdbID": "tt0816692",
                      "Type": "movie",
                      "Poster": "https://example.com/interstellar.jpg"
                    }
                  ],
                  "totalResults": "25",
                  "Response": "True"
                }
                """, MediaType.APPLICATION_JSON));

        MovieSearchResult result = provider.searchMovies(
            "Inception",
            1,
            "en-US"
        );

        assertEquals(1, result.page());
        assertEquals(3, result.totalPages());
        assertEquals(25, result.totalResults());
        assertEquals(2, result.results().size());

        MovieSearchResult.MovieSearchItem first = result.results().getFirst();

        assertEquals("Inception", first.title());
        assertEquals("2010", first.releaseDate());
        assertEquals(
            "https://example.com/inception.jpg",
            first.posterUrl()
        );
        assertNull(first.providerId());
        assertNull(first.rating());

        server.verify();
    }

    @Test
    void searchMoviesReturnsEmptyResultsWhenNoMoviesMatch() {
        server.expect(requestTo(
                org.hamcrest.Matchers.containsString("s=UnknownMovie")
            ))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("""
                {
                  "Response": "False",
                  "Error": "Movie not found!",
                  "totalResults": "0"
                }
                """, MediaType.APPLICATION_JSON));

        MovieSearchResult result = provider.searchMovies(
            "UnknownMovie",
            1,
            "en-US"
        );

        assertEquals(1, result.page());
        assertEquals(0, result.totalPages());
        assertEquals(0, result.totalResults());
        assertTrue(result.results().isEmpty());

        server.verify();
    }

    @Test
    void searchMoviesNormalizesUnavailablePoster() {
        server.expect(requestTo(
                org.hamcrest.Matchers.containsString("s=NoPoster")
            ))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("""
                {
                  "Search": [
                    {
                      "Title": "No Poster",
                      "Year": "2020",
                      "imdbID": "tt1234567",
                      "Type": "movie",
                      "Poster": "N/A"
                    }
                  ],
                  "totalResults": "1",
                  "Response": "True"
                }
                """, MediaType.APPLICATION_JSON));

        MovieSearchResult result = provider.searchMovies(
            "NoPoster",
            1,
            "en-US"
        );

        assertEquals(1, result.results().size());
        assertNull(result.results().getFirst().posterUrl());

        server.verify();
    }

    @Test
    void getMovieDetailsReturnsEmptyBecauseOmdbUsesImdbIds() {
        assertTrue(provider.getMovieDetails(123L).isEmpty());
    }

    @Test
    void getMovieDetailsByImdbIdReturnsCompleteMovie() {
        server.expect(requestTo(
                org.hamcrest.Matchers.allOf(
                    org.hamcrest.Matchers.containsString("apikey=test-key"),
                    org.hamcrest.Matchers.containsString("i=tt0133093"),
                    org.hamcrest.Matchers.containsString("plot=full")
                )
            ))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("""
                {
                  "Title": "The Matrix",
                  "Year": "1999",
                  "Rated": "R",
                  "Runtime": "136 min",
                  "Genre": "Action, Sci-Fi",
                  "Director": "Lana Wachowski, Lilly Wachowski",
                  "Writer": "Lilly Wachowski, Lana Wachowski",
                  "Actors": "Keanu Reeves, Laurence Fishburne",
                  "Plot": "A computer hacker discovers reality is a simulation.",
                  "Poster": "https://example.com/matrix.jpg",
                  "imdbRating": "8.7",
                  "imdbID": "tt0133093",
                  "Response": "True"
                }
                """, MediaType.APPLICATION_JSON));

        Optional<Movie> result = provider.getMovieDetailsByImdbId("tt0133093");

        assertTrue(result.isPresent());

        Movie movie = result.get();

        assertEquals("The Matrix", movie.getTitle());
        assertEquals("tt0133093", movie.getImdbId());
        assertEquals(1999, movie.getReleaseYear());
        assertEquals(136, movie.getRuntime());
        assertEquals(8.7, movie.getRating());
        assertEquals("R", movie.getRated());
        assertEquals(
            "https://example.com/matrix.jpg",
            movie.getPosterUrl()
        );
        assertEquals(
            "Lana Wachowski, Lilly Wachowski",
            movie.getDirector()
        );
        assertEquals(
            "Lilly Wachowski, Lana Wachowski",
            movie.getWriters()
        );
        assertEquals(
            "Keanu Reeves, Laurence Fishburne",
            movie.getActors()
        );
        assertEquals(
            "A computer hacker discovers reality is a simulation.",
            movie.getDescription()
        );
        assertEquals(
            java.util.Set.of("Action", "Sci-Fi"),
            movie.getGenres()
        );

        server.verify();
    }

    @Test
    void getMovieDetailsByImdbIdReturnsEmptyWhenMovieIsNotFound() {
        server.expect(requestTo(
                org.hamcrest.Matchers.containsString("i=tt0000000")
            ))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("""
                {
                  "Response": "False",
                  "Error": "Movie not found!"
                }
                """, MediaType.APPLICATION_JSON));

        Optional<Movie> result = provider.getMovieDetailsByImdbId(
            "tt0000000"
        );

        assertTrue(result.isEmpty());

        server.verify();
    }

    @Test
    void getMovieDetailsByImdbIdHandlesUnavailableOptionalFields() {
        server.expect(requestTo(
                org.hamcrest.Matchers.containsString("i=tt7654321")
            ))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("""
                {
                  "Title": "Minimal Movie",
                  "Year": "N/A",
                  "Runtime": "N/A",
                  "Genre": "N/A",
                  "Director": "N/A",
                  "Writer": "N/A",
                  "Actors": "N/A",
                  "Plot": "N/A",
                  "Poster": "N/A",
                  "imdbRating": "N/A",
                  "imdbID": "tt7654321",
                  "Response": "True"
                }
                """, MediaType.APPLICATION_JSON));

        Movie movie = provider
            .getMovieDetailsByImdbId("tt7654321")
            .orElseThrow();
        assertEquals("Minimal Movie", movie.getTitle());
        assertNull(movie.getReleaseYear());
        assertNull(movie.getRuntime());
        assertTrue(movie.getGenres().isEmpty());
        assertNull(movie.getDirector());
        assertNull(movie.getWriters());
        assertNull(movie.getActors());
        assertNull(movie.getDescription());
        assertNull(movie.getPosterUrl());
        assertNull(movie.getRating());
        assertEquals("tt7654321", movie.getImdbId());
        assertNull(movie.getRated());

        server.verify();
    }

    @Test
    void searchMoviesThrowsWhenProviderIsNotConfigured() {
        properties.getOmdb().setApiKey("");

        assertThrows(
            MovieProviderException.class,
            () -> provider.searchMovies("Inception", 1, "en-US")
        );
    }

    @Test
    void getMovieDetailsByImdbIdThrowsWhenProviderIsNotConfigured() {
        properties.getOmdb().setApiKey("");

        assertThrows(
            MovieProviderException.class,
            () -> provider.getMovieDetailsByImdbId("tt0133093")
        );
    }
}
