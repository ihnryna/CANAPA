
package org.spring.canapa.backend.recommendation.strategy;

import org.spring.canapa.backend.recommendation.dto.MovieCandidate;
import org.spring.canapa.backend.recommendation.dto.PreferenceSnapshot;
import org.spring.canapa.backend.recommendation.dto.RecommendedMovie;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Component
public class AverageRatingStrategy implements RecommendationStrategy {

    @Override
    public RecommendationType type() {
        return RecommendationType.AVERAGE_RATING;
    }

    @Override
    public List<RecommendedMovie> recommend(
            List<MovieCandidate> movies,
            List<PreferenceSnapshot> preferences
    ) {
        Objects.requireNonNull(movies, "Movies must not be null");
        Objects.requireNonNull(preferences, "Preferences must not be null");

        return movies.stream()
                .filter(Objects::nonNull)
                .filter(movie -> isValidRating(movie.rating()))
                .map(movie -> new RecommendedMovie(
                        movie,
                        movie.rating() / 10.0
                ))
                .sorted(
                        Comparator.comparingDouble(RecommendedMovie::score)
                                .reversed()
                                .thenComparing(
                                        result -> result.movie().title(),
                                        String.CASE_INSENSITIVE_ORDER
                                )
                                .thenComparing(result -> result.movie().id())
                )
                .toList();
    }

    private boolean isValidRating(Double rating) {
        return rating != null
                && Double.isFinite(rating)
                && rating >= 0.0
                && rating <= 10.0;
    }
}
