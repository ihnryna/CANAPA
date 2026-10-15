package org.spring.canapa.backend.voting.controller;

import org.spring.canapa.backend.voting.dto.ExcludeMovieRequest;
import org.spring.canapa.backend.voting.dto.ExcludedMovieResponse;
import org.spring.canapa.backend.voting.entity.ExcludedMovie;
import org.spring.canapa.backend.voting.service.VotingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms/{roomId}/excluded-movies")
public class ExcludedMovieController {

    private final VotingService votingService;

    public ExcludedMovieController(VotingService votingService) {
        this.votingService = votingService;
    }

    @GetMapping
    public ResponseEntity<List<ExcludedMovieResponse>> getExcludedMovies(
            @PathVariable UUID roomId
    ) {
        List<ExcludedMovieResponse> movies = votingService
                .getExcludedMovies(roomId)
                .stream()
                .map(ExcludedMovieController::toResponse)
                .toList();

        return ResponseEntity.ok(movies);
    }

    @PostMapping
    public ResponseEntity<ExcludedMovieResponse> excludeMovie(
            @PathVariable UUID roomId,
            @RequestBody ExcludeMovieRequest request
    ) {
        if (request.userId() == null || request.movieId() == null) {
            throw new IllegalArgumentException(
                    "userId and movieId are required"
            );
        }

        ExcludedMovie movie = votingService.excludeMovie(
                request.userId(),
                roomId,
                request.movieId()
        );

        return ResponseEntity.ok(toResponse(movie));
    }

    @DeleteMapping("/{movieId}")
    public ResponseEntity<Void> restoreMovie(
            @PathVariable UUID roomId,
            @PathVariable Long movieId,
            @RequestParam UUID userId
    ) {
        votingService.restoreMovie(userId, roomId, movieId);
        return ResponseEntity.noContent().build();
    }

    private static ExcludedMovieResponse toResponse(
            ExcludedMovie movie
    ) {
        return new ExcludedMovieResponse(
                movie.getId(),
                movie.getRoomId(),
                movie.getMovieId(),
                movie.getExcludedByUserId(),
                movie.getExcludedAt()
        );
    }
}