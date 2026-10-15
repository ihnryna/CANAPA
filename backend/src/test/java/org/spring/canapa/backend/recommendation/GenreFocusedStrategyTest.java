
package org.spring.canapa.backend.recommendation;

import org.junit.jupiter.api.Test;
import org.spring.canapa.backend.recommendation.strategy.GenreFocusedStrategy;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.spring.canapa.backend.recommendation.RecommendationTestData.*;

class GenreFocusedStrategyTest {

    private final GenreFocusedStrategy strategy =
            new GenreFocusedStrategy();

    @Test
    void shouldRankMoviesByGroupGenrePreferences() {
        var preferences = List.of(
                preference(
                        Set.of("Sci-Fi", "Drama"),
                        Set.of(),
                        null,
                        null
                ),
                preference(
                        Set.of("Drama"),
                        Set.of("Horror"),
                        null,
                        null
                )
        );

        var movies = List.of(
                movie("The Martian", Set.of("Sci-Fi"), 8.0, 144),
                movie("Horror Movie", Set.of("Horror"), 7.0, 100),
                movie("Interstellar", Set.of("Sci-Fi", "Drama"), 8.7, 169)
        );

        var result = strategy.recommend(movies, preferences);

        assertEquals(3, result.size());

        assertEquals("Interstellar", result.get(0).movie().title());
        assertEquals(1.0, result.get(0).score(), 0.0001);

        assertEquals("The Martian", result.get(1).movie().title());
        assertEquals(0.25, result.get(1).score(), 0.0001);

        assertEquals("Horror Movie", result.get(2).movie().title());
        assertEquals(0.0, result.get(2).score(), 0.0001);
    }

    @Test
    void shouldIgnoreGenreCaseAndWhitespace() {
        var movies = List.of(
                movie("Test Movie", Set.of(" SCI-FI "), 8.0, 120)
        );

        var preferences = List.of(
                preference(Set.of("sci-fi"), Set.of(), null, null)
        );

        var result = strategy.recommend(movies, preferences);

        assertEquals(1, result.size());
        assertEquals(1.0, result.getFirst().score(), 0.0001);
    }

    @Test
    void shouldReturnEmptyListWithoutGenrePreferences() {
        var movies = List.of(
                movie("Interstellar", Set.of("Sci-Fi"), 8.7, 169)
        );

        var preferences = List.of(
                preference(Set.of(), Set.of(), null, null)
        );

        var result = strategy.recommend(movies, preferences);

        assertTrue(result.isEmpty());
    }
}
