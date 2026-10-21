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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserPreferenceServiceTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SECOND_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ROOM_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

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

        when(preferenceRepository.findByUserIdAndRoomId(USER_ID, ROOM_ID))
            .thenReturn(Optional.empty());

        when(preferenceRepository.save(any(UserPreference.class)))
            .thenAnswer(invocation -> invocation.<UserPreference>getArgument(0));

        var response = preferenceService.createOrUpdatePreference(
            USER_ID,
            ROOM_ID,
            request
        );

        assertEquals(USER_ID, response.userId());
        assertEquals(ROOM_ID, response.roomId());
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
            () -> preferenceService.createOrUpdatePreference(USER_ID, ROOM_ID, request)
        );

        verify(preferenceRepository, never()).save(any());
    }

    @Test
    void calculatesGroupPreferences() {
        UserPreference first = new UserPreference();
        first.setUserId(USER_ID);
        first.setRoomId(ROOM_ID);
        first.setLikedGenres(Set.of("Action", "Comedy"));
        first.setDislikedGenres(Set.of("Horror"));
        first.setMinRating(7.0);
        first.setMaxRuntime(120);
        first.setPreferredLanguage("en-us");

        UserPreference second = new UserPreference();
        second.setUserId(SECOND_USER_ID);
        second.setRoomId(ROOM_ID);
        second.setLikedGenres(Set.of("Comedy", "Drama"));
        second.setDislikedGenres(Set.of("Comedy"));
        second.setMinRating(8.0);
        second.setMaxRuntime(100);
        second.setPreferredLanguage("uk");

        when(preferenceRepository.findAllByRoomId(ROOM_ID))
            .thenReturn(java.util.List.of(first, second));

        var summary = preferenceService.getGroupPreferenceSummary(ROOM_ID);

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

        when(preferenceRepository.findByUserIdAndRoomId(USER_ID, ROOM_ID))
            .thenReturn(Optional.empty());

        when(preferenceRepository.save(any(UserPreference.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var response = preferenceService.createOrUpdatePreference(
            USER_ID,
            ROOM_ID,
            request
        );

        assertTrue(response.likedGenres().contains("Action"));
        assertFalse(response.likedGenres().contains("  Action  "));
    }
}
