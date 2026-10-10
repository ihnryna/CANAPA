package org.spring.canapa.backend.recommendation.strategy;

import org.spring.canapa.backend.recommendation.dto.MovieCandidate;
import org.spring.canapa.backend.recommendation.dto.PreferenceSnapshot;
import org.spring.canapa.backend.recommendation.dto.RecommendedMovie;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ConsensusStrategy implements RecommendationStrategy {

    private static final double MIN_WEIGHT = 0.7;
    private static final double AVERAGE_WEIGHT = 0.3;

    @Override
    public RecommendationType type() {
        return RecommendationType.CONSENSUS;
    }

    @Override
    public List<RecommendedMovie> recommend(
            List<MovieCandidate> movies,
            List<PreferenceSnapshot> preferences
    ) {
        Objects.requireNonNull(movies, "Movies must not be null");
        Objects.requireNonNull(preferences, "Preferences must not be null");

        List<PreferenceSnapshot> groupPreferences = preferences.stream()
                .filter(Objects::nonNull)
                .toList();

        if (groupPreferences.isEmpty()) {
            return List.of();
        }

        return movies.stream()
                .filter(Objects::nonNull)
                .filter(movie ->
                        satisfiesAllRestrictions(movie, groupPreferences)
                )
                .map(movie -> new RecommendedMovie(
                        movie,
                        calculateConsensusScore(movie, groupPreferences)
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

    private boolean satisfiesAllRestrictions(
            MovieCandidate movie,
            List<PreferenceSnapshot> preferences
    ) {
        Set<String> movieGenres = normalizeGenres(movie.genres());

        for (PreferenceSnapshot preference : preferences) {

            Set<String> dislikedGenres =
                    normalizeGenres(preference.dislikedGenres());

            boolean containsDislikedGenre = dislikedGenres.stream()
                    .anyMatch(movieGenres::contains);

            if (containsDislikedGenre) {
                return false;
            }

            if (preference.minRating() != null) {
                Double rating = movie.rating();

                if (rating == null
                        || !Double.isFinite(rating)
                        || rating < preference.minRating()) {
                    return false;
                }
            }

            if (preference.maxRuntime() != null) {
                Integer runtime = movie.runtime();

                if (runtime == null
                        || runtime > preference.maxRuntime()) {
                    return false;
                }
            }
        }

        return true;
    }

    private double calculateConsensusScore(
            MovieCandidate movie,
            List<PreferenceSnapshot> preferences
    ) {
        Set<String> movieGenres = normalizeGenres(movie.genres());

        DoubleSummaryStatistics statistics = preferences.stream()
                .mapToDouble(preference ->
                        calculateUserScore(movieGenres, preference)
                )
                .summaryStatistics();

        double minimumScore = statistics.getMin();
        double averageScore = statistics.getAverage();

        return MIN_WEIGHT * minimumScore
                + AVERAGE_WEIGHT * averageScore;
    }

    private double calculateUserScore(
            Set<String> movieGenres,
            PreferenceSnapshot preference
    ) {
        Set<String> likedGenres =
                normalizeGenres(preference.likedGenres());

        if (likedGenres.isEmpty()) {
            return 1.0;
        }

        long matchingGenres = likedGenres.stream()
                .filter(movieGenres::contains)
                .count();

        return (double) matchingGenres / likedGenres.size();
    }

    private Set<String> normalizeGenres(Set<String> genres) {
        if (genres == null || genres.isEmpty()) {
            return Set.of();
        }

        return genres.stream()
                .filter(Objects::nonNull)
                .map(genre -> genre.trim().toLowerCase(Locale.ROOT))
                .filter(genre -> !genre.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }
}
