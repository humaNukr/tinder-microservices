package com.tinder.chat.infrastructure.adapter.out.event;

import com.tinder.chat.application.port.out.message.MessageOutboxEventPort;
import com.tinder.chat.domain.model.Message;
import com.tinder.chat.shared.dto.event.MessageSavedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SpringOutboxEventAdapter implements MessageOutboxEventPort {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void publishMessageSavedEvent(Message message, UUID recipientId) {
        eventPublisher.publishEvent(new MessageSavedEvent(message, recipientId));
    }
}