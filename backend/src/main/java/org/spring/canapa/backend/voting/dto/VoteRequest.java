package org.spring.canapa.backend.voting.dto;

public record VoteRequest(
        Long userId,
        Long movieId
) {
}