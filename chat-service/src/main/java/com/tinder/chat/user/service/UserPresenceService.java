package com.tinder.chat.user.service;

import java.util.UUID;

public interface UserPresenceService {
    void userConnected(UUID userId, String sessionId);

    void userDisconnected(UUID userId, String sessionId);

    void handleGracePeriodExpired(UUID userId);

    boolean isUserOnline(UUID userId);
}
