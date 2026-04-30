package com.tinder.swipe.service.interfaces;

import java.util.UUID;

public interface SwipeRateLimiterService {
    void checkAndIncrementLikeLimit(UUID userId, boolean isPremium);
}