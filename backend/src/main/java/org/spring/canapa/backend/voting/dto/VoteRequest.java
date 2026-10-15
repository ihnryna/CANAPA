package org.spring.canapa.backend.voting.dto;

import java.util.UUID;

public record VoteRequest(
        UUID userId,
        Long movieId
) {
}