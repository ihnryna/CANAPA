package org.spring.canapa.backend.voting.dto;

import java.time.LocalDateTime;
import java.util.List;

public record VotingResultResponse(
        Long id,
        Long roomId,
        List<Long> winnerMovieIds,
        int voteCount,
        LocalDateTime finishedAt
) {
}