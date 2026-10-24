package org.spring.canapa.backend.movie.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.spring.canapa.backend.movie.config.MovieApiProperties;
import org.spring.canapa.backend.movie.dto.MovieDiscoveryCriteria;
import org.spring.canapa.backend.movie.dto.MovieSearchResult;
import org.spring.canapa.backend.movie.entity.Movie;
import org.spring.canapa.backend.movie.provider.exception.MovieProviderException;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.AssertionsKt.assertNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TmdbMovieProviderTest {

    private MovieApiProperties properties;
    private MockRestServiceServer server;
    private TmdbMovieProvider provider;

    @BeforeEach
    void setUp() {
        properties = new MovieApiProperties();
        properties.getTmdb().setBaseUrl("https://api.themoviedb.org/3");
        properties.getTmdb().setApiKey("test-key");

        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();

        provider = new TmdbMovieProvider(
            builder.build(),
            properties
        );
    }

    @Test
    void searchesMovies() {
        server.expect(requestTo(
                org.hamcrest.Matchers.containsString("/search/movie")
            ))
            .andExpect(method(org.springframework.http.HttpMethod.GET))
            .andRespond(withSuccess("""
                {
                  "page": 1,
                  "total_pages": 1,
                  "total_results": 1,
                  "results": [
                    {
                      "id": 27205,
                      "title": "Inception",
                      "overview": "A dream within a dream.",
                      "poster_path": "/poster.jpg",
                      "release_date": "2010-07-15",
                      "vote_average": 8.8,
                      "original_language": "en"
                    }
                  ]
                }
                """, MediaType.APPLICATION_JSON));

        MovieSearchResult result = provider.searchMovies("Inception", 1, "en-US");

        assertEquals(1, result.totalResults());
        assertEquals("Inception", result.results().getFirst().title());
        assertEquals(
            "https://image.tmdb.org/t/p/w500/poster.jpg",
            result.results().getFirst().posterUrl()
        );

        server.verify();
    }

    @Test
    void rejectsRequestsWhenNotConfigured() {
        properties.getTmdb().setApiKey("");
        properties.getTmdb().setAccessToken("");

        assertThrows(
            MovieProviderException.class,
            () -> provider.searchMovies("Inception", 1, "en-US")
        );
    }

    @Test
    void getsMovieDetailsAndMapsAllFields() {
        server.expect(requestTo(
                org.hamcrest.Matchers.containsString("/movie/27205")
            ))
            .andExpect(method(org.springframework.http.HttpMethod.GET))
            .andRespond(withSuccess("""
                {
                  "id": 27205,
                  "title": "Inception",
                  "overview": "A dream within a dream.",
                  "poster_path": "/poster.jpg",
                  "release_date": "2010-07-15",
                  "vote_average": 8.8,
                  "runtime": 148,
                  "original_language": "en",
                  "genres": [
                    {"id": 28, "name": "Action"},
                    {"id": 878, "name": "Science Fiction"}
                  ],
                  "external_ids": {
                    "imdb_id": "tt1375666"
                  }
                }
                """, MediaType.APPLICATION_JSON));

        Movie movie = provider.getMovieDetails(27205L).orElseThrow();

        assertEquals(27205L, movie.getTmdbId());
        assertEquals("tt1375666", movie.getImdbId());
        assertEquals("Inception", movie.getTitle());
        assertEquals("A dream within a dream.", movie.getDescription());
        assertEquals(8.8, movie.getRating());
        assertEquals(148, movie.getRuntime());
        assertEquals(2010, movie.getReleaseYear());
        assertEquals(
            "https://image.tmdb.org/t/p/w500/poster.jpg",
            movie.getPosterUrl()
        );
        assertEquals("en", movie.getOriginalLanguage());
        assertEquals(
            java.util.Set.of("Action", "Science Fiction"),
            movie.getGenres()
        );

        server.verify();
    }

    @Test
    void getMovieDetailsHandlesMissingOptionalFields() {
        server.expect(requestTo(
                org.hamcrest.Matchers.containsString("/movie/27205")
            ))
            .andExpect(method(org.springframework.http.HttpMethod.GET))
            .andRespond(withSuccess("""
                {
                  "id": 27205,
                  "title": "Minimal Movie",
                  "overview": null,
                  "poster_path": null,
                  "release_date": "",
                  "vote_average": null,
                  "runtime": 0,
                  "original_language": null
                }
                """, MediaType.APPLICATION_JSON));

        Movie movie = provider
            .getMovieDetails(27205L)
            .orElseThrow();
        assertEquals(27205L, movie.getTmdbId());
        assertEquals("Minimal Movie", movie.getTitle());
        assertNull(movie.getDescription());
        assertNull(movie.getPosterUrl());
        assertNull(movie.getReleaseYear());
        assertNull(movie.getRating());
        assertNull(movie.getRuntime());
        assertNull(movie.getOriginalLanguage());
        assertTrue(movie.getGenres().isEmpty());
        assertNull(movie.getDirector());
        assertNull(movie.getWriters());
        assertNull(movie.getActors());
        assertNull(movie.getRated());

        server.verify();
    }

    @Test
    void getsMovieDetailsByImdbId() {
        server.expect(requestTo(
                org.hamcrest.Matchers.containsString("/find/tt1375666")
            ))
            .andExpect(method(org.springframework.http.HttpMethod.GET))
            .andRespond(withSuccess("""
                {
                  "movie_results": [
                    {
                      "id": 27205,
                      "title": "Inception"
                    }
                  ]
                }
                """, MediaType.APPLICATION_JSON));

        server.expect(requestTo(
                org.hamcrest.Matchers.containsString("/movie/27205")
            ))
            .andExpect(method(org.springframework.http.HttpMethod.GET))
            .andRespond(withSuccess("""
                {
                  "id": 27205,
                  "title": "Inception",
                  "overview": "A dream within a dream.",
                  "release_date": "2010-07-15",
                  "vote_average": 8.8,
                  "runtime": 148,
                  "original_language": "en",
                  "genres": [{"id": 28, "name": "Action"}],
                  "external_ids": {"imdb_id": "tt1375666"}
                }
                """, MediaType.APPLICATION_JSON));

        Movie movie = provider.getMovieDetailsByImdbId("tt1375666")
            .orElseThrow();

        assertEquals("Inception", movie.getTitle());
        assertEquals(27205L, movie.getTmdbId());
        assertEquals("tt1375666", movie.getImdbId());
        assertEquals(2010, movie.getReleaseYear());
        assertEquals(java.util.Set.of("Action"), movie.getGenres());

        server.verify();
    }

    @Test
    void returnsEmptyWhenImdbIdHasNoMatchingMovie() {
        server.expect(requestTo(
                org.hamcrest.Matchers.containsString("/find/tt0000000")
            ))
            .andExpect(method(org.springframework.http.HttpMethod.GET))
            .andRespond(withSuccess("""
                {
                  "movie_results": []
                }
                """, MediaType.APPLICATION_JSON));

        assertTrue(
            provider.getMovieDetailsByImdbId("tt0000000").isEmpty()
        );

        server.verify();
    }

    @Test
    void searchesMoviesWithNoResults() {
        server.expect(requestTo(
                org.hamcrest.Matchers.containsString("/search/movie")
            ))
            .andExpect(method(org.springframework.http.HttpMethod.GET))
            .andRespond(withSuccess("""
                {
                  "page": 1,
                  "total_pages": 0,
                  "total_results": 0,
                  "results": []
                }
                """, MediaType.APPLICATION_JSON));

        MovieSearchResult result = provider.searchMovies(
            "No Such Movie",
            1,
            "en-US"
        );

        assertEquals(1, result.page());
        assertEquals(0, result.totalResults());
        assertEquals(0, result.totalPages());
        assertTrue(result.results().isEmpty());

        server.verify();
    }

    @Test
    void discoversMoviesUsingGroupPreferences() {
        server.expect(requestTo(
                org.hamcrest.Matchers.containsString("/discover/movie")
            ))
            .andExpect(queryParam("with_genres", "35,878"))
            .andExpect(queryParam("without_genres", "27"))
            .andExpect(queryParam("vote_average.gte", "7.0"))
            .andExpect(queryParam("with_runtime.lte", "150"))
            .andExpect(queryParam("with_original_language", "en"))
            .andRespond(withSuccess("""
                {
                  "page": 1,
                  "total_pages": 1,
                  "total_results": 1,
                  "results": [
                    {
                      "id": 157336,
                      "title": "Interstellar",
                      "overview": "Space exploration.",
                      "poster_path": "/poster.jpg",
                      "release_date": "2014-11-05",
                      "vote_average": 8.5,
                      "original_language": "en",
                      "genre_ids": [12, 18, 878]
                    }
                  ]
                }
                """, MediaType.APPLICATION_JSON));

        MovieSearchResult result = provider.discoverMovies(
            new MovieDiscoveryCriteria(
                java.util.Set.of("Comedy", "Science Fiction"),
                java.util.Set.of("Horror"),
                7.0,
                150,
                java.util.Set.of("en")
            ),
            1,
            "en"
        );

        assertEquals(1, result.results().size());
        assertEquals(
            List.of("Adventure", "Drama", "Science Fiction"),
            result.results().getFirst().genres()
        );
        server.verify();
    }

    @Test
    void usesBearerTokenWhenAccessTokenIsConfigured() {
        properties.getTmdb().setAccessToken("test-access-token");

        server.expect(requestTo(
                org.hamcrest.Matchers.containsString("/search/movie")
            ))
            .andExpect(method(org.springframework.http.HttpMethod.GET))
            .andExpect(header(
                "Authorization",
                "Bearer test-access-token"
            ))
            .andRespond(withSuccess("""
                {
                  "page": 1,
                  "total_pages": 1,
                  "total_results": 0,
                  "results": []
                }
                """, MediaType.APPLICATION_JSON));

        MovieSearchResult result = provider.searchMovies(
            "Inception",
            1,
            "en-US"
        );

        assertEquals(0, result.totalResults());

        server.verify();
    }
}
