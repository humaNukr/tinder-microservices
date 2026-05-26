package com.tinder.profile.event;

import java.time.Instant;
import java.util.UUID;

public record UserPresenceEvent(
        UUID userId,
        boolean isOnline,
        Instant timestamp
) {
}
