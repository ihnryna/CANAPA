package org.spring.canapa.backend.recommendation;

import org.spring.canapa.backend.recommendation.dto.MovieCandidate;
import org.spring.canapa.backend.recommendation.dto.PreferenceSnapshot;
import org.spring.canapa.backend.recommendation.dto.RecommendedMovie;
import org.spring.canapa.backend.recommendation.strategy.RecommendationStrategy;
import org.spring.canapa.backend.recommendation.strategy.RecommendationType;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class RecommendationService {

    private final Map<RecommendationType, RecommendationStrategy> strategies;

    public RecommendationService(
            List<RecommendationStrategy> strategyImplementations
    ) {
        Objects.requireNonNull(
                strategyImplementations,
                "Strategies must not be null"
        );

        EnumMap<RecommendationType, RecommendationStrategy> strategyMap =
                new EnumMap<>(RecommendationType.class);

        for (RecommendationStrategy strategy : strategyImplementations) {
            Objects.requireNonNull(
                    strategy,
                    "Strategy must not be null"
            );

            RecommendationType type = Objects.requireNonNull(
                    strategy.type(),
                    "Strategy type must not be null"
            );

            RecommendationStrategy previous =
                    strategyMap.putIfAbsent(type, strategy);

            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate recommendation strategy: " + type
                );
            }
        }

        this.strategies = Map.copyOf(strategyMap);
    }

    public List<RecommendedMovie> recommend(
            RecommendationType type,
            List<MovieCandidate> movies,
            List<PreferenceSnapshot> preferences
    ) {
        Objects.requireNonNull(
                type, "Recommendation type must not be null"
        );

        Objects.requireNonNull(
                movies, "Movies must not be null"
        );

        Objects.requireNonNull(
                preferences, "Preferences must not be null"
        );

        RecommendationStrategy strategy = strategies.get(type);

        if (strategy == null) {
            throw new IllegalArgumentException(
                    "Unsupported recommendation strategy: " + type
            );
        }

        return strategy.recommend(movies, preferences);
    }
}
