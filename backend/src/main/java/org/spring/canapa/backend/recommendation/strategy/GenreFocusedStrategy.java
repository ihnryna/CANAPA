
package org.spring.canapa.backend.recommendation.strategy;

import org.spring.canapa.backend.recommendation.dto.MovieCandidate;
import org.spring.canapa.backend.recommendation.dto.PreferenceSnapshot;
import org.spring.canapa.backend.recommendation.dto.RecommendedMovie;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class GenreFocusedStrategy implements RecommendationStrategy {

    @Override
    public RecommendationType type() {
        return RecommendationType.GENRE_FOCUSED;
    }

    @Override
    public List<RecommendedMovie> recommend(
            List<MovieCandidate> movies,
            List<PreferenceSnapshot> preferences
    ) {
        Objects.requireNonNull(movies, "Movies must not be null");
        Objects.requireNonNull(preferences, "Preferences must not be null");

        List<PreferenceSnapshot> genrePreferences = preferences.stream()
                .filter(Objects::nonNull)
                .filter(preference ->
                        !normalizeGenres(preference.likedGenres()).isEmpty()
                                || !normalizeGenres(preference.dislikedGenres()).isEmpty()
                )
                .toList();

        if (genrePreferences.isEmpty()) {
            return List.of();
        }

        return movies.stream()
                .filter(Objects::nonNull)
                .map(movie -> new RecommendedMovie(
                        movie,
                        calculateGroupScore(movie, genrePreferences)
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

    private double calculateGroupScore(
            MovieCandidate movie,
            List<PreferenceSnapshot> preferences
    ) {
        Set<String> movieGenres = normalizeGenres(movie.genres());

        if (movieGenres.isEmpty()) {
            return 0.0;
        }

        return preferences.stream()
                .mapToDouble(preference ->
                        calculateUserScore(movieGenres, preference)
                )
                .average()
                .orElse(0.0);
    }

    private double calculateUserScore(
            Set<String> movieGenres,
            PreferenceSnapshot preference
    ) {
        Set<String> likedGenres =
                normalizeGenres(preference.likedGenres());

        Set<String> dislikedGenres =
                normalizeGenres(preference.dislikedGenres());

        boolean hasDislikedGenre = dislikedGenres.stream()
                .anyMatch(movieGenres::contains);

        if (hasDislikedGenre) {
            return 0.0;
        }

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
