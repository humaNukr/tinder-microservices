package com.tinder.chat.infrastructure.outbox;

import com.tinder.chat.config.KafkaTopicsProperties;
import com.tinder.chat.infrastructure.kafka.contract.MessageSentEvent;
import com.tinder.chat.infrastructure.outbox.contract.MessageSavedEvent;
import com.tinder.chat.message.model.Message;
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

    private final OutboxRepository outboxRepository;
    private final KafkaTopicsProperties kafkaTopicsProperties;

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

        OutboxEvent outboxEvent = new OutboxEvent(kafkaTopicsProperties.messageEvents(), kafkaEvent, LocalDateTime.now());

        outboxRepository.save(outboxEvent);
        log.debug("Outbox event saved for message id: {}", message.getId());
    }
}