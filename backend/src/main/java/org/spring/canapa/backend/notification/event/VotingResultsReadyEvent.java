package org.spring.canapa.backend.notification.event;

import java.util.List;
import java.util.UUID;

public record VotingResultsReadyEvent(
    UUID roomId,
    List<Long> winnerMovieIds,
    int voteCount
) {
    public VotingResultsReadyEvent {
        winnerMovieIds = List.copyOf(winnerMovieIds);
    }
}
