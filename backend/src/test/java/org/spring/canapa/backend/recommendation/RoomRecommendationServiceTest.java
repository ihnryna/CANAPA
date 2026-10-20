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
}
