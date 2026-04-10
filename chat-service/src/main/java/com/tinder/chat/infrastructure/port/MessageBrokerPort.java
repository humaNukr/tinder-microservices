package com.tinder.chat.infrastructure.port;

import java.util.UUID;

public interface MessageBrokerPort {
    void sendEvent(String topic, UUID key, String payload) throws Exception;
}