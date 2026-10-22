package org.spring.canapa.backend.movie.dto;

import java.util.Set;

public record MovieDiscoveryCriteria(
    Set<String> likedGenres,
    Set<String> dislikedGenres,
    Double minimumRating,
    Integer maximumRuntime,
    Set<String> preferredLanguages
) {
    public MovieDiscoveryCriteria {
        likedGenres = likedGenres == null
            ? Set.of()
            : Set.copyOf(likedGenres);
        dislikedGenres = dislikedGenres == null
            ? Set.of()
            : Set.copyOf(dislikedGenres);
        preferredLanguages = preferredLanguages == null
            ? Set.of()
            : Set.copyOf(preferredLanguages);
    }
}
