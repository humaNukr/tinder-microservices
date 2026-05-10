package com.tinder.chat.application.service.presence;

import com.tinder.chat.application.port.in.presence.UpdatePresenceUseCase;
import com.tinder.chat.application.port.out.presence.PresenceEventPort;
import com.tinder.chat.application.port.out.presence.PresenceStatePort;
import com.tinder.chat.shared.dto.event.UserPresenceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserPresenceManagementService implements UpdatePresenceUseCase {

    private final PresenceStatePort presenceStatePort;
    private final PresenceEventPort presenceEventPort;

    @Override
    public void userConnected(UUID userId, String sessionId) {
        boolean isNewlyOnline = presenceStatePort.registerSessionAndClearGrace(userId, sessionId);
        if (isNewlyOnline) {
            publishEvents(userId, true);
        }
    }

    @Override
    public void userDisconnected(UUID userId, String sessionId) {
        boolean isNowOffline = presenceStatePort.unregisterSession(userId, sessionId);
        if (isNowOffline) {
            log.debug("User {} disconnected. Starting grace period.", userId);
            presenceStatePort.startGracePeriod(userId);
        }
    }

    @Override
    public void handleGracePeriodExpired(UUID userId) {
        if (presenceStatePort.isOffline(userId)) {
            log.debug("Grace period expired for user {}. Publishing offline event.", userId);
            publishEvents(userId, false);
        }
    }

    private void publishEvents(UUID userId, boolean isOnline) {
        UserPresenceEvent event = new UserPresenceEvent(userId, isOnline, LocalDateTime.now());

        presenceEventPort.broadcastToChatServers(event);

        if (!isOnline) {
            presenceEventPort.broadcastToSystem(event);
        }
    }
}