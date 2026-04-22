package com.tinder.profile.event;

import java.time.Instant;
import java.util.UUID;

public record UserActivityEvent(
        UUID userId,
        ActivityType type,
        Instant timestamp
) {}