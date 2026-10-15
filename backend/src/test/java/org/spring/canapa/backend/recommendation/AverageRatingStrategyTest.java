
package org.spring.canapa.backend.recommendation;

import org.junit.jupiter.api.Test;
import org.spring.canapa.backend.recommendation.strategy.AverageRatingStrategy;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.spring.canapa.backend.recommendation.RecommendationTestData.movie;

class AverageRatingStrategyTest {

    private final AverageRatingStrategy strategy =
            new AverageRatingStrategy();

    @Test
    void shouldSortMoviesByRatingDescending() {
        var movies = List.of(
                movie("Dune", Set.of("Sci-Fi"), 8.5, 155),
                movie("Interstellar", Set.of("Sci-Fi"), 8.7, 169),
                movie("Inception", Set.of("Action"), 8.8, 148)
        );

        var result = strategy.recommend(movies, List.of());

        assertEquals(3, result.size());
        assertEquals("Inception", result.get(0).movie().title());
        assertEquals("Interstellar", result.get(1).movie().title());
        assertEquals("Dune", result.get(2).movie().title());

        assertEquals(0.88, result.get(0).score(), 0.0001);
    }

    @Test
    void shouldIgnoreMoviesWithInvalidRatings() {
        var movies = List.of(
                movie("Valid", Set.of("Drama"), 8.0, 120),
                movie("No Rating", Set.of("Drama"), null, 120),
                movie("Invalid Rating", Set.of("Drama"), 11.0, 120),
                movie("NaN Rating", Set.of("Drama"), Double.NaN, 120)
        );

        var result = strategy.recommend(movies, List.of());

        assertEquals(1, result.size());
        assertEquals("Valid", result.getFirst().movie().title());
    }

    @Test
    void shouldReturnEmptyListWhenNoMovies() {
        var result = strategy.recommend(List.of(), List.of());

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldRejectNullMovieList() {
        assertThrows(
                NullPointerException.class,
                () -> strategy.recommend(null, List.of())
        );
    }
}
