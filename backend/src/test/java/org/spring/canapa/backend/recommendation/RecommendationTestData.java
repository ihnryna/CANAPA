
package org.spring.canapa.backend.recommendation;

import org.spring.canapa.backend.recommendation.dto.MovieCandidate;
import org.spring.canapa.backend.recommendation.dto.PreferenceSnapshot;

import java.util.Set;
import java.util.UUID;

final class RecommendationTestData {

    private RecommendationTestData() {
    }

    static MovieCandidate movie(
            String title,
            Set<String> genres,
            Double rating,
            Integer runtime
    ) {
        return new MovieCandidate(
                UUID.randomUUID(),
                title,
                "Test description",
                genres,
                rating,
                runtime,
                2024,
                null
        );
    }

    static PreferenceSnapshot preference(
            Set<String> likedGenres,
            Set<String> dislikedGenres,
            Double minRating,
            Integer maxRuntime
    ) {
        return new PreferenceSnapshot(
                UUID.randomUUID(),
                UUID.randomUUID(),
                likedGenres,
                dislikedGenres,
                minRating,
                maxRuntime,
                null,
                null
        );
    }
}
