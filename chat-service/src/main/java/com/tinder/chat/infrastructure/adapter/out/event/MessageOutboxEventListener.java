package com.tinder.chat.infrastructure.adapter.out.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.chat.domain.event.MessageSavedEvent;
import com.tinder.chat.domain.model.Message;
import com.tinder.chat.infrastructure.adapter.out.persistence.outbox.OutboxEventEntity;
import com.tinder.chat.infrastructure.adapter.out.persistence.outbox.OutboxJpaRepository;
import com.tinder.chat.infrastructure.config.properties.KafkaTopicsProperties;
import com.tinder.chat.shared.dto.event.MessageSentEvent;
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

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleMessageSavedEvent(MessageSavedEvent event) {
        Message message = event.savedMessage();
        UUID recipientId = event.recipientId();

        String content = message.getContent();
        String snippet = (content != null && content.length() > 50)
                ? content.substring(0, 50) + "..."
                : content;

        UUID eventId = UUID.randomUUID();

        MessageSentEvent kafkaEvent = new MessageSentEvent(
                eventId,
                message.getId(),
                message.getChatId(),
                message.getSenderId(),
                recipientId,
                message.getContentType().name(),
                snippet,
                message.getCreatedAt()
        );

        try {
            String jsonPayload = objectMapper.writeValueAsString(kafkaEvent);

            OutboxEventEntity outboxEvent = new OutboxEventEntity(
                    kafkaTopicsProperties.messageEvents(),
                    jsonPayload,
                    LocalDateTime.now()
            );

            outboxRepository.save(outboxEvent);
            log.debug("Outbox event saved for message id: {}", message.getId());

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize kafka event for Outbox. Message ID: {}", message.getId(), e);
            throw new RuntimeException("Serialization error during Outbox event creation", e);
        }
    }
}