package com.tinder.swipe.event;

import java.time.Instant;
import java.util.UUID;

public record SwipeCreatedEvent(
        UUID swiperId,
        UUID swipedId,
        boolean isLiked,
        Instant timestamp
) {}