
package org.spring.canapa.backend.recommendation;

import org.junit.jupiter.api.Test;
import org.spring.canapa.backend.recommendation.strategy.AverageRatingStrategy;
import org.spring.canapa.backend.recommendation.strategy.ConsensusStrategy;
import org.spring.canapa.backend.recommendation.strategy.GenreFocusedStrategy;
import org.spring.canapa.backend.recommendation.strategy.RecommendationType;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.spring.canapa.backend.recommendation.RecommendationTestData.movie;

class RecommendationServiceTest {

    private final RecommendationService service =
            new RecommendationService(List.of(
                    new AverageRatingStrategy(),
                    new GenreFocusedStrategy(),
                    new ConsensusStrategy()
            ));

    @Test
    void shouldSelectAverageRatingStrategy() {
        var movies = List.of(
                movie("Dune", Set.of("Sci-Fi"), 8.5, 155),
                movie("Inception", Set.of("Action"), 8.8, 148)
        );

        var result = service.recommend(
                RecommendationType.AVERAGE_RATING,
                movies,
                List.of()
        );

        assertEquals(2, result.size());
        assertEquals("Inception", result.getFirst().movie().title());
    }

    @Test
    void shouldSelectGenreFocusedStrategy() {
        var movies = List.of(
                movie("Dune", Set.of("Sci-Fi"), 8.5, 155)
        );

        var result = service.recommend(
                RecommendationType.GENRE_FOCUSED,
                movies,
                List.of()
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldRejectUnsupportedStrategy() {
        var incompleteService = new RecommendationService(
                List.of(new AverageRatingStrategy())
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> incompleteService.recommend(
                        RecommendationType.CONSENSUS,
                        List.of(),
                        List.of()
                )
        );
    }

    @Test
    void shouldRejectDuplicateStrategyTypes() {
        assertThrows(
                IllegalStateException.class,
                () -> new RecommendationService(List.of(
                        new AverageRatingStrategy(),
                        new AverageRatingStrategy()
                ))
        );
    }

    @Test
    void shouldRejectNullRecommendationType() {
        assertThrows(
                NullPointerException.class,
                () -> service.recommend(
                        null,
                        List.of(),
                        List.of()
                )
        );
    }
}
