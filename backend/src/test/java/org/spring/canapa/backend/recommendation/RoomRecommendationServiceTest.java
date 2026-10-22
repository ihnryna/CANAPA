package org.spring.canapa.backend.recommendation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.spring.canapa.backend.movie.dto.MovieResponse;
import org.spring.canapa.backend.movie.dto.MovieDiscoveryCriteria;
import org.spring.canapa.backend.movie.entity.Movie;
import org.spring.canapa.backend.movie.repository.MovieRepository;
import org.spring.canapa.backend.movie.service.MovieService;
import org.spring.canapa.backend.preference.dto.UserPreferenceResponse;
import org.spring.canapa.backend.preference.service.UserPreferenceService;
import org.spring.canapa.backend.recommendation.strategy.*;
import org.spring.canapa.backend.room.RoomService;
import org.spring.canapa.backend.room.RoomStatus;
import org.spring.canapa.backend.room.Room;
import org.spring.canapa.backend.room.RoomRepository;
import org.spring.canapa.backend.room.dto.RoomData;
import org.spring.canapa.backend.room.dto.RoomMemberData;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RoomRecommendationServiceTest {

    private final RoomService roomService = mock(RoomService.class);
    private final RoomRepository roomRepository = mock(RoomRepository.class);
    private final MovieService movieService = mock(MovieService.class);
    private final MovieRepository movieRepository = mock(MovieRepository.class);
    private final UserPreferenceService preferenceService =
        mock(UserPreferenceService.class);

    private final RoomRecommendationService service =
        new RoomRecommendationService(
            roomService,
            roomRepository,
            movieService,
            movieRepository,
            preferenceService,
            new RecommendationService(List.of(
                new ConsensusStrategy(),
                new AverageRatingStrategy(),
                new GenreFocusedStrategy()
            ))
        );

    private final UUID roomId = UUID.randomUUID();
    private final UUID memberId = UUID.randomUUID();
    private final Room lockedRoom = mock(Room.class);

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
        when(roomRepository.findByIdForUpdate(roomId))
            .thenReturn(Optional.of(lockedRoom));
        when(lockedRoom.getMovieCandidates()).thenReturn(List.of());
        when(movieRepository.findAllById(any()))
            .thenReturn(List.of());
    }

    @Test
    void shouldReturnMovieWithScore() {
        when(movieService.discoverMovies(any(MovieDiscoveryCriteria.class), eq(40)))
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
            .discoverMovies(any(MovieDiscoveryCriteria.class), anyInt());
    }

    @Test
    void shouldReturnEmptyWithoutPreferences() {
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

        when(movieService.discoverMovies(any(MovieDiscoveryCriteria.class), eq(40)))
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

        when(movieService.discoverMovies(any(MovieDiscoveryCriteria.class), eq(40)))
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
        when(movieService.discoverMovies(any(MovieDiscoveryCriteria.class), eq(40)))
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

    @Test
    void shouldBuildTmdbCriteriaFromAllRoomParticipants() {
        UUID secondMemberId = UUID.randomUUID();
        RoomMemberData firstMember = new RoomMemberData(
            memberId, "Vika", "vika@example.com"
        );
        RoomMemberData secondMember = new RoomMemberData(
            secondMemberId, "Taras", "taras@example.com"
        );
        when(roomService.getRoom(roomId)).thenReturn(new RoomData(
            roomId,
            "Movie night",
            firstMember,
            List.of(firstMember, secondMember),
            RoomStatus.CREATED,
            Instant.now()
        ));
        when(preferenceService.getRoomPreferences(roomId)).thenReturn(List.of(
            new UserPreferenceResponse(
                1L, memberId, roomId,
                Set.of("Comedy", "Science Fiction"), Set.of("Horror"),
                6.5, 180, "en", null
            ),
            new UserPreferenceResponse(
                2L, secondMemberId, roomId,
                Set.of("Drama", "Horror"), Set.of("Comedy"),
                7.5, 140, "en", null
            )
        ));
        when(movieService.discoverMovies(any(MovieDiscoveryCriteria.class), eq(40)))
            .thenReturn(List.of());

        service.recommend(
            roomId, memberId, RecommendationType.CONSENSUS, 20
        );

        var criteriaCaptor = org.mockito.ArgumentCaptor
            .forClass(MovieDiscoveryCriteria.class);
        verify(movieService).discoverMovies(criteriaCaptor.capture(), eq(40));

        MovieDiscoveryCriteria criteria = criteriaCaptor.getValue();
        assertEquals(Set.of("Science Fiction", "Drama"), criteria.likedGenres());
        assertEquals(Set.of("Horror", "Comedy"), criteria.dislikedGenres());
        assertEquals(7.5, criteria.minimumRating());
        assertEquals(140, criteria.maximumRuntime());
        assertEquals(Set.of("en"), criteria.preferredLanguages());
    }

    @Test
    void shouldReuseMoviesCachedForRoomWithoutCallingProviders() {
        Movie cachedMovie = new Movie();
        cachedMovie.setId(77L);
        cachedMovie.setTitle("Cached movie");
        cachedMovie.setDescription("Already stored");
        cachedMovie.setGenres(Set.of("Drama"));
        cachedMovie.setRating(8.0);
        cachedMovie.setRuntime(120);
        when(lockedRoom.getMovieCandidates())
            .thenReturn(List.of(cachedMovie));
        when(lockedRoom.hasMovieCandidatesCache()).thenReturn(true);
        when(preferenceService.getRoomPreferences(roomId))
            .thenReturn(List.of(new UserPreferenceResponse(
                1L, memberId, roomId,
                Set.of("Drama"), Set.of(), 7.0, 150, "en", null
            )));

        var result = service.recommend(
            roomId, memberId, RecommendationType.CONSENSUS, 40
        );

        assertEquals(1, result.size());
        assertEquals(77L, result.getFirst().movie().id());
        verify(movieService, never())
            .discoverMovies(any(MovieDiscoveryCriteria.class), anyInt());
    }

    @Test
    void shouldCacheAnEmptyDiscoveryResult() {
        when(lockedRoom.hasMovieCandidatesCache())
            .thenReturn(false, true);
        when(preferenceService.getRoomPreferences(roomId))
            .thenReturn(List.of(new UserPreferenceResponse(
                1L, memberId, roomId,
                Set.of("Documentary"), Set.of(), 10.0, 1, "uk", null
            )));
        when(movieService.discoverMovies(
            any(MovieDiscoveryCriteria.class), eq(40)
        )).thenReturn(List.of());

        service.recommend(
            roomId, memberId, RecommendationType.CONSENSUS, 40
        );
        service.recommend(
            roomId, memberId, RecommendationType.CONSENSUS, 40
        );

        verify(movieService, times(1)).discoverMovies(
            any(MovieDiscoveryCriteria.class), eq(40)
        );
        verify(lockedRoom).cacheMovieCandidates(List.of());
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
