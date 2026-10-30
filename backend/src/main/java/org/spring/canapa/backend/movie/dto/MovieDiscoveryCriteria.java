package org.spring.canapa.backend.movie.dto;

import java.util.List;
import java.util.Set;

public record MovieDiscoveryCriteria(
    Set<String> likedGenres,
    Set<String> dislikedGenres,
    Double minimumRating,
    Integer maximumRuntime,
    Set<String> preferredLanguages,
    List<Set<String>> likedGenreGroups
) {
    public MovieDiscoveryCriteria(
        Set<String> likedGenres,
        Set<String> dislikedGenres,
        Double minimumRating,
        Integer maximumRuntime,
        Set<String> preferredLanguages
    ) {
        this(
            likedGenres,
            dislikedGenres,
            minimumRating,
            maximumRuntime,
            preferredLanguages,
            likedGenres == null || likedGenres.isEmpty()
                ? List.of()
                : List.of(Set.copyOf(likedGenres))
        );
    }

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
        likedGenreGroups = likedGenreGroups == null
            ? (likedGenres.isEmpty()
                ? List.of()
                : List.of(likedGenres))
            : likedGenreGroups.stream()
                .map(group -> group == null ? Set.<String>of() : Set.copyOf(group))
                .toList();
    }
}
