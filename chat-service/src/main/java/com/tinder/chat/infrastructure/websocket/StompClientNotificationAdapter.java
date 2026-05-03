package com.tinder.chat.infrastructure.websocket;

import com.tinder.chat.chat.port.ClientNotificationPort;
import com.tinder.chat.config.WebSocketProperties;
import com.tinder.chat.message.dto.MessageAckDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompClientNotificationAdapter implements ClientNotificationPort {

    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketProperties webSocketProperties;

    @Override
    public void sendAck(UUID userId, MessageAckDto ackDto) {
        log.debug("Sending message ACK to user {}: localId={}", userId, ackDto.localId());

        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                webSocketProperties.queueAcks(),
                ackDto
        );
    }
}