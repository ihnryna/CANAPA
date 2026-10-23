package org.spring.canapa.backend.recommendation;

import org.spring.canapa.backend.movie.dto.MovieDiscoveryCriteria;
import org.spring.canapa.backend.movie.dto.MovieResponse;
import org.spring.canapa.backend.movie.entity.Movie;
import org.spring.canapa.backend.movie.repository.MovieRepository;
import org.spring.canapa.backend.movie.service.MovieService;
import org.spring.canapa.backend.preference.dto.UserPreferenceResponse;
import org.spring.canapa.backend.preference.service.UserPreferenceService;
import org.spring.canapa.backend.recommendation.dto.MovieCandidate;
import org.spring.canapa.backend.recommendation.dto.PreferenceSnapshot;
import org.spring.canapa.backend.recommendation.dto.RoomRecommendedMovie;
import org.spring.canapa.backend.recommendation.strategy.RecommendationType;
import org.spring.canapa.backend.room.RoomService;
import org.spring.canapa.backend.room.Room;
import org.spring.canapa.backend.room.RoomRepository;
import org.spring.canapa.backend.room.dto.RoomData;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RoomRecommendationService {

    private static final int ROOM_MOVIE_CACHE_SIZE = 15;

    private final RoomService roomService;
    private final RoomRepository roomRepository;
    private final MovieService movieService;
    private final MovieRepository movieRepository;
    private final UserPreferenceService preferenceService;
    private final RecommendationService recommendationService;

    public RoomRecommendationService(
        RoomService roomService,
        RoomRepository roomRepository,
        MovieService movieService,
        MovieRepository movieRepository,
        UserPreferenceService preferenceService,
        RecommendationService recommendationService
    ) {
        this.roomService = roomService;
        this.roomRepository = roomRepository;
        this.movieService = movieService;
        this.movieRepository = movieRepository;
        this.preferenceService = preferenceService;
        this.recommendationService = recommendationService;
    }

    @Transactional
    public List<RoomRecommendedMovie> recommend(
        UUID roomId,
        UUID userId,
        RecommendationType strategy,
        int limit
    ) {
        if (strategy == null) {
            throw new IllegalArgumentException(
                "Recommendation strategy is required"
            );
        }

        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException(
                "Limit must be between 1 and 100"
            );
        }

        // Lock the room before it is loaded into the current persistence
        // context by another query. Otherwise concurrent recommendation
        // requests can both retain a stale, uncached Room instance and try to
        // insert the same room_movie_candidates rows.
        Room lockedRoom = roomRepository
            .findByIdForUpdate(roomId)
            .orElseThrow(() -> new IllegalStateException(
                "Room disappeared while loading recommendations: " + roomId
            ));

        RoomData room = roomService.getRoom(roomId);

        Set<UUID> participantIds = room.participants().stream()
            .map(member -> member.id())
            .collect(Collectors.toSet());

        if (userId == null || !participantIds.contains(userId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Only room participants can view recommendations"
            );
        }

        List<PreferenceSnapshot> preferences =
            preferenceService.getRoomPreferences(roomId)
                .stream()
                .filter(preference ->
                    participantIds.contains(preference.userId())
                )
                .map(this::toSnapshot)
                .toList();

        if (preferences.isEmpty()) {
            return List.of();
        }

        List<MovieResponse> movies;

        if (!lockedRoom.hasMovieCandidatesCache()) {
            movies = movieService.discoverMovies(
                discoveryCriteria(preferences),
                ROOM_MOVIE_CACHE_SIZE
            );
            cacheMovies(lockedRoom, movies);
        } else {
            movies = lockedRoom.getMovieCandidates().stream()
                .map(MovieResponse::from)
                .toList();
        }

        Map<UUID, MovieResponse> moviesByCandidateId =
            movies.stream().collect(Collectors.toMap(
                movie -> candidateId(movie.id()),
                Function.identity()
            ));

        List<MovieCandidate> candidates = movies.stream()
            .map(this::toCandidate)
            .toList();

        return recommendationService
            .recommend(strategy, candidates, preferences)
            .stream()
            .map(result -> new RoomRecommendedMovie(
                moviesByCandidateId.get(result.movie().id()),
                result.score()
            ))
            .limit(limit)
            .toList();
    }

    private void cacheMovies(Room room, List<MovieResponse> movies) {
        Map<Long, Movie> storedMovies = movieRepository
            .findAllById(movies.stream().map(MovieResponse::id).toList())
            .stream()
            .collect(Collectors.toMap(Movie::getId, Function.identity()));

        List<Movie> orderedMovies = movies.stream()
            .map(MovieResponse::id)
            .map(storedMovies::get)
            .filter(java.util.Objects::nonNull)
            .toList();

        room.cacheMovieCandidates(orderedMovies);
        roomRepository.saveAndFlush(room);
    }

    private MovieCandidate toCandidate(MovieResponse movie) {
        return new MovieCandidate(
            candidateId(movie.id()),
            movie.title(),
            movie.description(),
            movie.genres(),
            movie.rating(),
            movie.runtime(),
            movie.releaseYear(),
            movie.posterUrl()
        );
    }

    private PreferenceSnapshot toSnapshot(
        UserPreferenceResponse preference
    ) {
        return new PreferenceSnapshot(
            preference.userId(),
            preference.roomId(),
            preference.likedGenres(),
            preference.dislikedGenres(),
            preference.minRating(),
            preference.maxRuntime(),
            preference.preferredLanguage(),
            preference.textPreference()
        );
    }

    private MovieDiscoveryCriteria discoveryCriteria(
        List<PreferenceSnapshot> preferences
    ) {
        Set<String> likedGenres = new HashSet<>();
        Set<String> dislikedGenres = new HashSet<>();
        Set<String> languages = new HashSet<>();
        Double minimumRating = null;
        Integer maximumRuntime = null;

        for (PreferenceSnapshot preference : preferences) {
            likedGenres.addAll(preference.likedGenres());
            dislikedGenres.addAll(preference.dislikedGenres());

            if (preference.preferredLanguage() != null
                && !preference.preferredLanguage().isBlank()) {
                languages.add(preference.preferredLanguage().trim());
            }
            if (preference.minRating() != null) {
                minimumRating = minimumRating == null
                    ? preference.minRating()
                    : Math.max(minimumRating, preference.minRating());
            }
            if (preference.maxRuntime() != null) {
                maximumRuntime = maximumRuntime == null
                    ? preference.maxRuntime()
                    : Math.min(maximumRuntime, preference.maxRuntime());
            }
        }

        likedGenres.removeAll(dislikedGenres);

        return new MovieDiscoveryCriteria(
            likedGenres,
            dislikedGenres,
            minimumRating,
            maximumRuntime,
            languages
        );
    }

    private UUID candidateId(Long id) {
        return UUID.nameUUIDFromBytes(
            ("canapa-movie:" + id)
                .getBytes(StandardCharsets.UTF_8)
        );
    }
}
