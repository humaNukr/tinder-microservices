package com.tinder.chat.chat.port;

import com.tinder.chat.message.dto.MessageAckDto;

import java.util.UUID;

public interface ClientNotificationPort {
    void sendAck(UUID userId, MessageAckDto ackDto);
}