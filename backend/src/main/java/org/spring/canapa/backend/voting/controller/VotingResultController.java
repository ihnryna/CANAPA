package org.spring.canapa.backend.voting.controller;

import org.spring.canapa.backend.movie.entity.Movie;
import org.spring.canapa.backend.movie.dto.MovieResponse;
import org.spring.canapa.backend.voting.dto.VotingResultResponse;
import org.spring.canapa.backend.voting.entity.VotingResult;
import org.spring.canapa.backend.voting.repository.VotingResultRepository;
import org.spring.canapa.backend.voting.service.VotingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms/{roomId}/voting-result")
public class VotingResultController {

    private final VotingService votingService;
    private final VotingResultRepository votingResultRepository;

    public VotingResultController(
        VotingService votingService,
        VotingResultRepository votingResultRepository
    ) {
        this.votingService = votingService;
        this.votingResultRepository = votingResultRepository;
    }

    @PostMapping
    public ResponseEntity<VotingResultResponse> finishVoting(
        @PathVariable UUID roomId
    ) {
        VotingResult result = votingService.finishVoting(roomId);

        return ResponseEntity.ok(toResponse(result));
    }

    @GetMapping
    public ResponseEntity<VotingResultResponse> getVotingResult(
        @PathVariable UUID roomId
    ) {
        VotingResult result = votingResultRepository.findByRoom_Id(roomId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Voting result not found for this room"
            ));

        return ResponseEntity.ok(toResponse(result));
    }

    private static VotingResultResponse toResponse(VotingResult result) {
        Set<Long> seenMovieIds = new HashSet<>();
        List<Movie> winners = result.getWinners()
            .stream()
            .filter(movie -> seenMovieIds.add(movie.getId()))
            .toList();
        List<Long> winnerMovieIds = winners.stream().map(Movie::getId).toList();

        return new VotingResultResponse(
            result.getId(),
            result.getRoom().getId(),
            winnerMovieIds,
            winners.stream().map(MovieResponse::from).toList(),
            result.getVoteCount(),
            result.getFinishedAt()
        );
    }
}
