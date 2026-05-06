package com.tinder.chat.chat.port;

import com.tinder.chat.infrastructure.kafka.contract.UserPresenceEvent;

public interface UserPresencePublisher {
    void publishUserPresenceEvent(UserPresenceEvent event);
}
