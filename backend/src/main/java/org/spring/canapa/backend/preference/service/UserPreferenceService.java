package org.spring.canapa.backend.preference.service;

import org.spring.canapa.backend.preference.dto.UserPreferenceRequest;
import org.spring.canapa.backend.preference.dto.UserPreferenceResponse;
import org.spring.canapa.backend.preference.entity.UserPreference;
import org.spring.canapa.backend.preference.repository.UserPreferenceRepository;
import org.spring.canapa.backend.preference.service.exception.UserPreferenceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class UserPreferenceService {

    private final UserPreferenceRepository preferenceRepository;

    public UserPreferenceService(UserPreferenceRepository preferenceRepository) {
        this.preferenceRepository = preferenceRepository;
    }

    @Transactional
    public UserPreferenceResponse createOrUpdatePreference(
        UUID userId,
        UUID roomId,
        UserPreferenceRequest request
    ) {
        validateIds(userId, roomId);
        validatePreference(request);

        UserPreference preference = preferenceRepository
            .findByUserIdAndRoomId(userId, roomId)
            .orElseGet(() -> {
                UserPreference newPreference = new UserPreference();
                newPreference.setUserId(userId);
                newPreference.setRoomId(roomId);
                return newPreference;
            });

        preference.setLikedGenres(normalizeGenres(request.likedGenres()));
        preference.setDislikedGenres(normalizeGenres(request.dislikedGenres()));
        preference.setMinRating(request.minRating());
        preference.setMaxRuntime(request.maxRuntime());
        preference.setPreferredLanguage(normalizeLanguage(
            request.preferredLanguage()
        ));
        preference.setTextPreference(normalizeText(request.textPreference()));

        Set<String> liked = new HashSet<>(preference.getLikedGenres());
        liked.retainAll(preference.getDislikedGenres());
        if (!liked.isEmpty()) {
            throw new IllegalArgumentException(
                "A genre cannot be both liked and disliked: " + liked
            );
        }

        return UserPreferenceResponse.from(preferenceRepository.save(preference));
    }

    @Transactional(readOnly = true)
    public UserPreferenceResponse getPreference(UUID userId, UUID roomId) {
        validateIds(userId, roomId);

        UserPreference preference = preferenceRepository
            .findByUserIdAndRoomId(userId, roomId)
            .orElseThrow(() -> new UserPreferenceNotFoundException(
                "Preferences not found for user " + userId
                    + " in room " + roomId
            ));

        return UserPreferenceResponse.from(preference);
    }

    @Transactional(readOnly = true)
    public List<UserPreferenceResponse> getRoomPreferences(UUID roomId) {
        requireId(roomId, "Room");

        return preferenceRepository.findAllByRoomId(roomId)
            .stream()
            .map(UserPreferenceResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public GroupPreferenceSummary getGroupPreferenceSummary(UUID roomId) {
        requireId(roomId, "Room");

        List<UserPreference> preferences =
            preferenceRepository.findAllByRoomId(roomId);

        Set<String> likedGenres = new HashSet<>();
        Set<String> dislikedGenres = new HashSet<>();
        Set<String> languages = new HashSet<>();

        Double minRating = null;
        Integer maxRuntime = null;
        int usersWithRating = 0;
        int usersWithRuntime = 0;

        for (UserPreference preference : preferences) {
            likedGenres.addAll(preference.getLikedGenres());
            dislikedGenres.addAll(preference.getDislikedGenres());

            if (preference.getPreferredLanguage() != null) {
                languages.add(preference.getPreferredLanguage());
            }

            if (preference.getMinRating() != null) {
                minRating = minRating == null
                    ? preference.getMinRating()
                    : Math.max(minRating, preference.getMinRating());
                usersWithRating++;
            }

            if (preference.getMaxRuntime() != null) {
                maxRuntime = maxRuntime == null
                    ? preference.getMaxRuntime()
                    : Math.min(maxRuntime, preference.getMaxRuntime());
                usersWithRuntime++;
            }
        }

        likedGenres.removeAll(dislikedGenres);

        return new GroupPreferenceSummary(
            roomId,
            preferences.size(),
            likedGenres,
            dislikedGenres,
            minRating,
            maxRuntime,
            languages,
            usersWithRating,
            usersWithRuntime
        );
    }

    @Transactional
    public void deletePreference(UUID userId, UUID roomId) {
        validateIds(userId, roomId);

        if (!preferenceRepository.existsByUserIdAndRoomId(userId, roomId)) {
            throw new UserPreferenceNotFoundException(
                "Preferences not found for user " + userId
                    + " in room " + roomId
            );
        }

        preferenceRepository.deleteByUserIdAndRoomId(userId, roomId);
    }

    private void validateIds(UUID userId, UUID roomId) {
        requireId(userId, "User");
        requireId(roomId, "Room");
    }

    private void requireId(UUID id, String entityName) {
        if (id == null) {
            throw new IllegalArgumentException(entityName + " ID is required");
        }
    }

    private void validatePreference(UserPreferenceRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Preference request is required");
        }

        if (request.minRating() != null
            && (request.minRating() < 0 || request.minRating() > 10)) {
            throw new IllegalArgumentException(
                "Minimum rating must be between 0 and 10"
            );
        }

        if (request.maxRuntime() != null
            && (request.maxRuntime() < 1 || request.maxRuntime() > 600)) {
            throw new IllegalArgumentException(
                "Maximum runtime must be between 1 and 600 minutes"
            );
        }

        Set<String> liked = normalizeGenres(request.likedGenres());
        Set<String> disliked = normalizeGenres(request.dislikedGenres());
        liked.retainAll(disliked);

        if (!liked.isEmpty()) {
            throw new IllegalArgumentException(
                "A genre cannot be both liked and disliked: " + liked
            );
        }
    }

    private Set<String> normalizeGenres(Set<String> genres) {
        if (genres == null) {
            return new HashSet<>();
        }

        Set<String> normalized = new HashSet<>();

        for (String genre : genres) {
            if (genre == null || genre.isBlank()) {
                continue;
            }

            String value = genre.trim();

            if (value.length() > 100) {
                throw new IllegalArgumentException(
                    "Genre names cannot exceed 100 characters"
                );
            }

            normalized.add(value);
        }

        return normalized;
    }

    private String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return null;
        }

        String normalized = language.trim().replace('_', '-');

        if (normalized.length() > 10) {
            throw new IllegalArgumentException(
                "Preferred language cannot exceed 10 characters"
            );
        }

        return normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        String normalized = text.trim();

        if (normalized.length() > 2000) {
            throw new IllegalArgumentException(
                "Text preference cannot exceed 2000 characters"
            );
        }

        return normalized;
    }

    public record GroupPreferenceSummary(
        UUID roomId,
        int participantCount,
        Set<String> likedGenres,
        Set<String> dislikedGenres,
        Double minimumRating,
        Integer maximumRuntime,
        Set<String> preferredLanguages,
        int participantsWithMinimumRating,
        int participantsWithMaximumRuntime
    ) {
    }
}
