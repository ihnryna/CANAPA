package org.spring.canapa.backend.preference.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UserPreferenceRequest(
        @Size(max = 30)
        Set<@Size(min = 1, max = 100) String> likedGenres,

        @Size(max = 30)
        Set<@Size(min = 1, max = 100) String> dislikedGenres,

        @DecimalMin("0.0")
        @DecimalMax("10.0")
        Double minRating,

        @Min(1)
        @Max(600)
        Integer maxRuntime,

        @Size(max = 10)
        String preferredLanguage,

        @Size(max = 2000)
        String textPreference
) {
}
