package com.tinder.profile.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserPresenceEvent(
        UUID userId,
        boolean isOnline,
        LocalDateTime timestamp
) {
}