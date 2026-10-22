package org.spring.canapa.backend.voting.dto;

import org.spring.canapa.backend.movie.dto.MovieResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record VotingResultResponse(
    Long id,
    UUID roomId,
    List<Long> winnerMovieIds,
    List<MovieResponse> winners,
    int voteCount,
    LocalDateTime finishedAt
) {
}
