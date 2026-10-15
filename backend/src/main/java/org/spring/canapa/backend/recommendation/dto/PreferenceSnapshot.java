package org.spring.canapa.backend.recommendation.dto;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record PreferenceSnapshot(
        UUID userId,
        UUID roomId,
        Set<String> likedGenres,
        Set<String> dislikedGenres,
        Double minRating,
        Integer maxRuntime,
        String preferredLanguage,
        String textPreference
) {
    public PreferenceSnapshot {
        Objects.requireNonNull(
                userId, "User ID is required"
        );

        Objects.requireNonNull(
                roomId, "Room ID is required"
        );

        likedGenres = likedGenres == null
                ? Set.of()
                : Set.copyOf(likedGenres);

        dislikedGenres = dislikedGenres == null
                ? Set.of()
                : Set.copyOf(dislikedGenres);
    }
}