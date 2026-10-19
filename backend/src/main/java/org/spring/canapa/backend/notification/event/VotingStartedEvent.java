package org.spring.canapa.backend.notification.event;

import java.util.UUID;

public record VotingStartedEvent(UUID roomId) {
}
