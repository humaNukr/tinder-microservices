package com.tinder.chat.application.port.out.presence;

import java.util.UUID;

public interface PresenceStatePort {
    boolean registerSessionAndClearGrace(UUID userId, String sessionId);

    boolean unregisterSession(UUID userId, String sessionId);

    void startGracePeriod(UUID userId);

    boolean isOffline(UUID userId);
}