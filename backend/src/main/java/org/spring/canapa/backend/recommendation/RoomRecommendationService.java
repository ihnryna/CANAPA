package org.spring.canapa.backend.recommendation;

import org.spring.canapa.backend.movie.dto.MovieResponse;
import org.spring.canapa.backend.movie.service.MovieService;
import org.spring.canapa.backend.preference.dto.UserPreferenceResponse;
import org.spring.canapa.backend.preference.service.UserPreferenceService;
import org.spring.canapa.backend.recommendation.dto.MovieCandidate;
import org.spring.canapa.backend.recommendation.dto.PreferenceSnapshot;
import org.spring.canapa.backend.recommendation.dto.RoomRecommendedMovie;
import org.spring.canapa.backend.recommendation.strategy.RecommendationType;
import org.spring.canapa.backend.room.RoomService;
import org.spring.canapa.backend.room.dto.RoomData;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RoomRecommendationService {

    private final RoomService roomService;
    private final MovieService movieService;
    private final UserPreferenceService preferenceService;
    private final RecommendationService recommendationService;

    public RoomRecommendationService(
            RoomService roomService,
            MovieService movieService,
            UserPreferenceService preferenceService,
            RecommendationService recommendationService
    ) {
        this.roomService = roomService;
        this.movieService = movieService;
        this.preferenceService = preferenceService;
        this.recommendationService = recommendationService;
    }

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

        List<MovieResponse> movies =
                movieService.getPopularLocalMovies(limit);

        List<PreferenceSnapshot> preferences =
                preferenceService.getRoomPreferences(roomId)
                        .stream()
                        .filter(preference ->
                                participantIds.contains(preference.userId())
                        )
                        .map(this::toSnapshot)
                        .toList();

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
                .toList();
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

    private UUID candidateId(Long id) {
        return UUID.nameUUIDFromBytes(
                ("canapa-movie:" + id)
                        .getBytes(StandardCharsets.UTF_8)
        );
    }
}
