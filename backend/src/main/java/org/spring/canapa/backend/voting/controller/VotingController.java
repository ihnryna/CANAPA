package org.spring.canapa.backend.voting.controller;

import org.spring.canapa.backend.voting.dto.VoteRequest;
import org.spring.canapa.backend.voting.dto.VoteResponse;
import org.spring.canapa.backend.voting.entity.Vote;
import org.spring.canapa.backend.voting.service.VotingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms/{roomId}/votes")
public class VotingController {

    private final VotingService votingService;

    public VotingController(VotingService votingService) {
        this.votingService = votingService;
    }

    @PostMapping
    public ResponseEntity<VoteResponse> addVote(
            @PathVariable UUID roomId,
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
            @PathVariable UUID roomId,
            @PathVariable Long movieId,
            @RequestParam UUID userId
    ) {
        votingService.removeVote(userId, roomId, movieId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<VoteResponse>> getRoomVotes(
            @PathVariable UUID roomId
    ) {
        List<VoteResponse> votes = votingService.getRoomVotes(roomId)
                .stream()
                .map(VotingController::toResponse)
                .toList();

        return ResponseEntity.ok(votes);
    }

    @GetMapping("/me")
    public ResponseEntity<List<VoteResponse>> getUserVotes(
            @PathVariable UUID roomId,
            @RequestParam UUID userId
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
                vote.getUser().getId(),
                vote.getRoom().getId(),
                vote.getMovie().getId(),
                vote.getCreatedAt()
        );
    }
}