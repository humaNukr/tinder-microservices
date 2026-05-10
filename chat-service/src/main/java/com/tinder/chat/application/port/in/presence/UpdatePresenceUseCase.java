package com.tinder.chat.application.port.in.presence;

import java.util.UUID;

public interface UpdatePresenceUseCase {
    void userConnected(UUID userId, String sessionId);

    void userDisconnected(UUID userId, String sessionId);

    void handleGracePeriodExpired(UUID userId);
}