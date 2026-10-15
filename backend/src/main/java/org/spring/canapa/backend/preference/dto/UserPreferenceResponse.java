package org.spring.canapa.backend.preference.dto;

import org.spring.canapa.backend.preference.entity.UserPreference;

import java.util.Set;

public record UserPreferenceResponse(
        Long id,
        Long userId,
        Long roomId,
        Set<String> likedGenres,
        Set<String> dislikedGenres,
        Double minRating,
        Integer maxRuntime,
        String preferredLanguage,
        String textPreference
) {
    public static UserPreferenceResponse from(UserPreference preference) {
        return new UserPreferenceResponse(
                preference.getId(),
                preference.getUserId(),
                preference.getRoomId(),
                Set.copyOf(preference.getLikedGenres()),
                Set.copyOf(preference.getDislikedGenres()),
                preference.getMinRating(),
                preference.getMaxRuntime(),
                preference.getPreferredLanguage(),
                preference.getTextPreference()
        );
    }
}
