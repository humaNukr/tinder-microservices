package com.tinder.chat.application.port.out.presence;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface UserPresencePort {
    boolean isUserOnline(UUID userId);

    Map<UUID, Boolean> getPresenceBatch(Set<UUID> userIds);
}