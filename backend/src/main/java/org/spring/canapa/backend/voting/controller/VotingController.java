package org.spring.canapa.backend.voting.controller;

import org.spring.canapa.backend.voting.dto.VoteRequest;
import org.spring.canapa.backend.voting.dto.VoteResponse;
import org.spring.canapa.backend.voting.entity.Vote;
import org.spring.canapa.backend.voting.service.VotingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms/{roomId}/votes")
public class VotingController {

    private final VotingService votingService;

    public VotingController(VotingService votingService) {
        this.votingService = votingService;
    }

    @PostMapping
    public ResponseEntity<VoteResponse> addVote(
            @PathVariable Long roomId,
            @RequestBody VoteRequest request
    ) {
        if (request.userId() == null || request.movieId() == null) {
            throw new IllegalArgumentException(
                    "userId and movieId are required"
            );
        }

        Vote vote = votingService.addVote(
                request.userId(),
                roomId,
                request.movieId()
        );

        return ResponseEntity.ok(toResponse(vote));
    }

    @DeleteMapping("/{movieId}")
    public ResponseEntity<Void> removeVote(
            @PathVariable Long roomId,
            @PathVariable Long movieId,
            @RequestParam Long userId
    ) {
        votingService.removeVote(userId, roomId, movieId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<VoteResponse>> getRoomVotes(
            @PathVariable Long roomId
    ) {
        List<VoteResponse> votes = votingService.getRoomVotes(roomId)
                .stream()
                .map(VotingController::toResponse)
                .toList();

        return ResponseEntity.ok(votes);
    }

    @GetMapping("/me")
    public ResponseEntity<List<VoteResponse>> getUserVotes(
            @PathVariable Long roomId,
            @RequestParam Long userId
    ) {
        List<VoteResponse> votes = votingService
                .getUserVotes(userId, roomId)
                .stream()
                .map(VotingController::toResponse)
                .toList();

        return ResponseEntity.ok(votes);
    }

    private static VoteResponse toResponse(Vote vote) {
        return new VoteResponse(
                vote.getId(),
                vote.getUserId(),
                vote.getRoomId(),
                vote.getMovieId(),
                vote.getCreatedAt()
        );
    }
}