package org.spring.canapa.backend.movie.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.spring.canapa.backend.movie.config.MovieApiProperties;
import org.spring.canapa.backend.movie.dto.MovieSearchResult;
import org.spring.canapa.backend.movie.provider.exception.MovieProviderException;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
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
}
