package org.spring.canapa.backend.recommendation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.spring.canapa.backend.movie.dto.MovieResponse;
import org.spring.canapa.backend.movie.service.MovieService;
import org.spring.canapa.backend.preference.dto.UserPreferenceResponse;
import org.spring.canapa.backend.preference.service.UserPreferenceService;
import org.spring.canapa.backend.recommendation.strategy.*;
import org.spring.canapa.backend.room.RoomService;
import org.spring.canapa.backend.room.RoomStatus;
import org.spring.canapa.backend.room.dto.RoomData;
import org.spring.canapa.backend.room.dto.RoomMemberData;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class RoomRecommendationServiceTest {

    private final RoomService roomService = mock(RoomService.class);
    private final MovieService movieService = mock(MovieService.class);
    private final UserPreferenceService preferenceService =
        mock(UserPreferenceService.class);

    private final RoomRecommendationService service =
        new RoomRecommendationService(
            roomService,
            movieService,
            preferenceService,
            new RecommendationService(List.of(
                new ConsensusStrategy(),
                new AverageRatingStrategy(),
                new GenreFocusedStrategy()
            ))
        );

    private final UUID roomId = UUID.randomUUID();
    private final UUID memberId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        RoomMemberData member = new RoomMemberData(
            memberId, "Vika", "vika@example.com"
        );

        when(roomService.getRoom(roomId)).thenReturn(new RoomData(
            roomId,
            "Movie night",
            member,
            List.of(member),
            RoomStatus.CREATED,
            Instant.now()
        ));
    }

    @Test
    void shouldReturnMovieWithScore() {
        when(movieService.getPopularLocalMovies(40))
            .thenReturn(List.of(movie(7L, "Dune")));

        when(preferenceService.getRoomPreferences(roomId))
            .thenReturn(List.of(new UserPreferenceResponse(
                1L,
                memberId,
                roomId,
                Set.of("Sci-Fi"),
                Set.of(),
                7.0,
                180,
                null,
                null
            )));

        var result = service.recommend(
            roomId, memberId, RecommendationType.CONSENSUS, 40
        );

        assertEquals(1, result.size());
        assertEquals(7L, result.getFirst().movie().id());
        assertEquals(1.0, result.getFirst().score(), 0.00001);
    }

    @Test
    void shouldRejectNonParticipant() {
        var exception = assertThrows(
            ResponseStatusException.class,
            () -> service.recommend(
                roomId,
                UUID.randomUUID(),
                RecommendationType.CONSENSUS,
                40
            )
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(movieService, never())
            .getPopularLocalMovies(anyInt());
    }

    @Test
    void shouldReturnEmptyWithoutPreferences() {
        when(movieService.getPopularLocalMovies(40))
            .thenReturn(List.of(movie(7L, "Dune")));

        when(preferenceService.getRoomPreferences(roomId))
            .thenReturn(List.of());

        var result = service.recommend(
            roomId, memberId, RecommendationType.CONSENSUS, 40
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldRejectInvalidLimit() {
        assertThrows(IllegalArgumentException.class, () ->
            service.recommend(
                roomId,
                memberId,
                RecommendationType.AVERAGE_RATING,
                101
            )
        );
    }


    @Test
    void shouldReturnExpectedResultsForAllStrategies() {
        var space = testMovie(
            101L, "Space Adventure", Set.of("Sci-Fi"), 8.7, 135
        );
        var drama = testMovie(
            102L, "Drama Night", Set.of("Drama"), 9.3, 110
        );
        var horror = testMovie(
            103L, "Horror Story", Set.of("Horror"), 7.9, 185
        );

        when(movieService.getPopularLocalMovies(40))
            .thenReturn(List.of(space, drama, horror));

        when(preferenceService.getRoomPreferences(roomId))
            .thenReturn(List.of(new UserPreferenceResponse(
                1L,
                memberId,
                roomId,
                Set.of("Sci-Fi"),
                Set.of("Horror"),
                7.0,
                160,
                null,
                null
            )));

        var consensus = service.recommend(
            roomId, memberId, RecommendationType.CONSENSUS, 40
        );

        assertEquals(2, consensus.size());
        assertEquals("Space Adventure", consensus.get(0).movie().title());
        assertEquals(1.0, consensus.get(0).score(), 0.00001);
        assertEquals("Drama Night", consensus.get(1).movie().title());
        assertEquals(0.0, consensus.get(1).score(), 0.00001);

        var average = service.recommend(
            roomId, memberId, RecommendationType.AVERAGE_RATING, 40
        );

        assertEquals(3, average.size());
        assertEquals("Drama Night", average.get(0).movie().title());
        assertEquals(0.93, average.get(0).score(), 0.00001);
        assertEquals("Space Adventure", average.get(1).movie().title());
        assertEquals(0.87, average.get(1).score(), 0.00001);
        assertEquals("Horror Story", average.get(2).movie().title());
        assertEquals(0.79, average.get(2).score(), 0.00001);

        var genre = service.recommend(
            roomId, memberId, RecommendationType.GENRE_FOCUSED, 40
        );

        assertEquals(3, genre.size());
        assertEquals("Space Adventure", genre.get(0).movie().title());
        assertEquals(1.0, genre.get(0).score(), 0.00001);
        assertEquals(0.0, genre.get(1).score(), 0.00001);
        assertEquals(0.0, genre.get(2).score(), 0.00001);
    }

    @Test
    void shouldIgnorePreferencesOfNonParticipants() {
        UUID outsiderId = UUID.randomUUID();

        when(movieService.getPopularLocalMovies(40))
            .thenReturn(List.of(
                testMovie(101L, "Space", Set.of("Sci-Fi"), 8.7, 135)
            ));

        when(preferenceService.getRoomPreferences(roomId))
            .thenReturn(List.of(
                new UserPreferenceResponse(
                    1L, memberId, roomId,
                    Set.of("Sci-Fi"), Set.of(),
                    null, null, null, null
                ),
                new UserPreferenceResponse(
                    2L, outsiderId, roomId,
                    Set.of("Horror"), Set.of("Sci-Fi"),
                    null, null, null, null
                )
            ));

        var result = service.recommend(
            roomId, memberId, RecommendationType.CONSENSUS, 40
        );

        assertEquals(1, result.size());
        assertEquals(1.0, result.getFirst().score(), 0.00001);
    }

    @Test
    void shouldReturnEmptyResultsForEmptyMovieCatalog() {
        when(movieService.getPopularLocalMovies(40))
            .thenReturn(List.of());

        when(preferenceService.getRoomPreferences(roomId))
            .thenReturn(List.of());

        for (RecommendationType type : RecommendationType.values()) {
            var result = service.recommend(
                roomId, memberId, type, 40
            );

            assertTrue(result.isEmpty());
        }
    }

    private MovieResponse movie(long id, String title) {
        return new MovieResponse(
            id,
            null,
            null,
            title,
            "Description",
            Set.of("Sci-Fi"),
            8.5,
            150,
            2024,
            null,
            "en",
            null,
            null,
            null,
            null
        );
    }


    private MovieResponse testMovie(
        long id,
        String title,
        Set<String> genres,
        double rating,
        int runtime
    ) {
        return new MovieResponse(
            id,
            null,
            null,
            title,
            "Test description",
            genres,
            rating,
            runtime,
            2024,
            null,
            "en",
            null,
            null,
            null,
            null
        );
    }
}
