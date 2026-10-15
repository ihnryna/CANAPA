package org.spring.canapa.backend.preference.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spring.canapa.backend.preference.dto.UserPreferenceRequest;
import org.spring.canapa.backend.preference.entity.UserPreference;
import org.spring.canapa.backend.preference.repository.UserPreferenceRepository;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserPreferenceServiceTest {

    @Mock
    private UserPreferenceRepository preferenceRepository;

    private UserPreferenceService preferenceService;

    @BeforeEach
    void setUp() {
        preferenceService = new UserPreferenceService(preferenceRepository);
    }

    @Test
    void createsPreference() {
        UserPreferenceRequest request = new UserPreferenceRequest(
                Set.of("Action", "Comedy"),
                Set.of("Horror"),
                7.0,
                120,
                "en-US",
                "Something funny"
        );

        when(preferenceRepository.findByUserIdAndRoomId(1L, 10L))
                .thenReturn(Optional.empty());

        when(preferenceRepository.save(any(UserPreference.class)))
                .thenAnswer(invocation -> invocation.<UserPreference>getArgument(0));

        var response = preferenceService.createOrUpdatePreference(
                1L,
                10L,
                request
        );

        assertEquals(1L, response.userId());
        assertEquals(10L, response.roomId());
        assertEquals(Set.of("Action", "Comedy"), response.likedGenres());
        assertEquals(Set.of("Horror"), response.dislikedGenres());
        assertEquals(7.0, response.minRating());
        assertEquals(120, response.maxRuntime());
    }

    @Test
    void rejectsGenreThatIsBothLikedAndDisliked() {
        UserPreferenceRequest request = new UserPreferenceRequest(
                Set.of("Action"),
                Set.of("Action"),
                7.0,
                120,
                "en-US",
                null
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> preferenceService.createOrUpdatePreference(1L, 10L, request)
        );

        verify(preferenceRepository, never()).save(any());
    }

    @Test
    void calculatesGroupPreferences() {
        UserPreference first = new UserPreference();
        first.setUserId(1L);
        first.setRoomId(10L);
        first.setLikedGenres(Set.of("Action", "Comedy"));
        first.setDislikedGenres(Set.of("Horror"));
        first.setMinRating(7.0);
        first.setMaxRuntime(120);
        first.setPreferredLanguage("en-us");

        UserPreference second = new UserPreference();
        second.setUserId(2L);
        second.setRoomId(10L);
        second.setLikedGenres(Set.of("Comedy", "Drama"));
        second.setDislikedGenres(Set.of("Comedy"));
        second.setMinRating(8.0);
        second.setMaxRuntime(100);
        second.setPreferredLanguage("uk");

        when(preferenceRepository.findAllByRoomId(10L))
                .thenReturn(java.util.List.of(first, second));

        var summary = preferenceService.getGroupPreferenceSummary(10L);

        assertEquals(2, summary.participantCount());
        assertEquals(Set.of("Action", "Drama"), summary.likedGenres());
        assertEquals(Set.of("Horror", "Comedy"), summary.dislikedGenres());
        assertEquals(8.0, summary.minimumRating());
        assertEquals(100, summary.maximumRuntime());
        assertEquals(Set.of("en-us", "uk"), summary.preferredLanguages());
    }

    @Test
    void normalizesGenres() {
        UserPreferenceRequest request = new UserPreferenceRequest(
                new HashSet<>(Set.of("  Action  ", "Comedy")),
                Set.of(),
                null,
                null,
                null,
                null
        );

        when(preferenceRepository.findByUserIdAndRoomId(1L, 10L))
                .thenReturn(Optional.empty());

        when(preferenceRepository.save(any(UserPreference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = preferenceService.createOrUpdatePreference(
                1L,
                10L,
                request
        );

        assertTrue(response.likedGenres().contains("Action"));
        assertFalse(response.likedGenres().contains("  Action  "));
    }
}
