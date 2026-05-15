package com.tinder.chat.infrastructure.adapter.out.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.chat.domain.event.MessageSavedEvent;
import com.tinder.chat.domain.model.Message;
import com.tinder.chat.infrastructure.adapter.out.persistence.outbox.OutboxEventEntity;
import com.tinder.chat.infrastructure.adapter.out.persistence.outbox.OutboxJpaRepository;
import com.tinder.chat.infrastructure.config.properties.KafkaTopicsProperties;
import com.tinder.chat.shared.dto.event.MessageSentEvent;
import com.tinder.chat.shared.mapper.MessageEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageOutboxEventListener {

    private final OutboxJpaRepository outboxRepository;
    private final KafkaTopicsProperties kafkaTopicsProperties;
    private final ObjectMapper objectMapper;
    private final MessageEventMapper messageEventMapper;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleMessageSavedEvent(MessageSavedEvent event) {
        Message message = event.savedMessage();

        String content = message.getContent();
        String snippet = (content != null && content.length() > 50)
                ? content.substring(0, 50) + "..."
                : content;

        MessageSentEvent kafkaEvent = messageEventMapper.toMessageSentEvent(
                message,
                UUID.randomUUID(),
                event.recipientId(),
                snippet
        );

        try {
            String jsonPayload = objectMapper.writeValueAsString(kafkaEvent);

            OutboxEventEntity outboxEvent = new OutboxEventEntity(
                    kafkaTopicsProperties.messageEvents(),
                    jsonPayload,
                    LocalDateTime.now()
            );

            outboxRepository.save(outboxEvent);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize kafka event for Outbox. Message ID: {}", message.getId(), e);
            throw new RuntimeException("Serialization error during Outbox event creation", e);
        }
    }
}