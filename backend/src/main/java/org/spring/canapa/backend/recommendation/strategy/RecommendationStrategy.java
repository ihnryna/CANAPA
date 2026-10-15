package org.spring.canapa.backend.recommendation.strategy;

import org.spring.canapa.backend.recommendation.dto.MovieCandidate;
import org.spring.canapa.backend.recommendation.dto.PreferenceSnapshot;
import org.spring.canapa.backend.recommendation.dto.RecommendedMovie;

import java.util.List;

public interface RecommendationStrategy {

    RecommendationType type();

    List<RecommendedMovie> recommend(
            List<MovieCandidate> movies,
            List<PreferenceSnapshot> preferences
    );
}