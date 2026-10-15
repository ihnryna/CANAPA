package org.spring.canapa.backend.voting.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record VotingResultResponse(
        Long id,
        UUID roomId,
        List<Long> winnerMovieIds,
        int voteCount,
        LocalDateTime finishedAt
) {
}