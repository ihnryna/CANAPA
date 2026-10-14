package org.spring.canapa.backend.voting.controller;

import org.spring.canapa.backend.voting.dto.VotingResultResponse;
import org.spring.canapa.backend.voting.entity.VotingResult;
import org.spring.canapa.backend.voting.repository.VotingResultRepository;
import org.spring.canapa.backend.voting.service.VotingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

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
            @PathVariable Long roomId
    ) {
        VotingResult result = votingService.finishVoting(roomId);

        return ResponseEntity.ok(toResponse(result));
    }

    @GetMapping
    public ResponseEntity<VotingResultResponse> getVotingResult(
            @PathVariable Long roomId
    ) {
        VotingResult result = votingResultRepository.findByRoomId(roomId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Voting result not found for this room"
                ));

        return ResponseEntity.ok(toResponse(result));
    }

    private static VotingResultResponse toResponse(
            VotingResult result
    ) {
        return new VotingResultResponse(
                result.getId(),
                result.getRoomId(),
                List.copyOf(result.getWinnerMovieIds()),
                result.getVoteCount(),
                result.getFinishedAt()
        );
    }
}