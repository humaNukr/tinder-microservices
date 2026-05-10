package com.tinder.chat.application.port.out.presence;

import com.tinder.chat.shared.dto.event.UserPresenceEvent;

public interface PresenceEventPort {
    void broadcastToChatServers(UserPresenceEvent event);

    void broadcastToSystem(UserPresenceEvent event);
}