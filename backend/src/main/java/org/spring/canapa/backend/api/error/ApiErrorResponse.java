package org.spring.canapa.backend.api.error;

import java.time.Instant;

public record ApiErrorResponse(
        ApiErrorCode code,
        String message,
        int status,
        String path,
        Instant timestamp
) {
}
