
package org.spring.canapa.backend.recommendation;

import org.junit.jupiter.api.Test;
import org.spring.canapa.backend.recommendation.strategy.ConsensusStrategy;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.spring.canapa.backend.recommendation.RecommendationTestData.*;

class ConsensusStrategyTest {

    private final ConsensusStrategy strategy =
            new ConsensusStrategy();

    @Test
    void shouldRespectAllParticipantRestrictions() {
        var preferences = List.of(
                preference(
                        Set.of("Sci-Fi"),
                        Set.of("Horror"),
                        7.0,
                        180
                ),
                preference(
                        Set.of("Drama"),
                        Set.of(),
                        7.5,
                        150
                )
        );

        var movies = List.of(
                movie("Balanced", Set.of("Sci-Fi", "Drama"), 8.1, 140),
                movie("Too Long", Set.of("Sci-Fi", "Drama"), 8.1, 160),
                movie("Low Rated", Set.of("Sci-Fi", "Drama"), 7.2, 120),
                movie("Blocked Horror", Set.of("Sci-Fi", "Horror"), 8.2, 120),
                movie("Only Sci-Fi", Set.of("Sci-Fi"), 8.0, 125)
        );

        var result = strategy.recommend(movies, preferences);

        assertEquals(2, result.size());

        assertEquals("Balanced", result.get(0).movie().title());
        assertEquals(1.0, result.get(0).score(), 0.0001);

        assertEquals("Only Sci-Fi", result.get(1).movie().title());
        assertEquals(0.15, result.get(1).score(), 0.0001);
    }

    @Test
    void shouldRejectMovieWithMissingRequiredRating() {
        var movies = List.of(
                movie("No Rating", Set.of("Drama"), null, 120)
        );

        var preferences = List.of(
                preference(Set.of("Drama"), Set.of(), 7.0, null)
        );

        var result = strategy.recommend(movies, preferences);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldRejectMovieWithMissingRequiredRuntime() {
        var movies = List.of(
                movie("No Runtime", Set.of("Drama"), 8.0, null)
        );

        var preferences = List.of(
                preference(Set.of("Drama"), Set.of(), null, 150)
        );

        var result = strategy.recommend(movies, preferences);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyListWithoutParticipants() {
        var movies = List.of(
                movie("Interstellar", Set.of("Sci-Fi"), 8.7, 169)
        );

        var result = strategy.recommend(movies, List.of());

        assertTrue(result.isEmpty());
    }
}
