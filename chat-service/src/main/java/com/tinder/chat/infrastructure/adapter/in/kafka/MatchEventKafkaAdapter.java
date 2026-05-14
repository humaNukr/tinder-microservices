package com.tinder.chat.infrastructure.adapter.in.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.chat.application.port.in.room.CreateChatUseCase;
import com.tinder.chat.infrastructure.adapter.out.persistence.inbox.InboxEventEntity;
import com.tinder.chat.infrastructure.adapter.out.persistence.inbox.InboxEventJpaRepository;
import com.tinder.chat.shared.dto.event.MatchEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchEventKafkaAdapter {

    private final InboxEventJpaRepository inboxEventRepository;
    private final CreateChatUseCase createChatUseCase;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    @KafkaListener(
            topics = "${app.kafka.topics.match-events}",
            groupId = "${app.kafka.consumer-groups.chat-service}"
    )
    public void listenAndProcessMatchEvent(String eventJson) {
        try {
            MatchEvent event = objectMapper.readValue(eventJson, MatchEvent.class);

            transactionTemplate.executeWithoutResult(status -> {
                if (inboxEventRepository.existsByEventId(event.eventId())) {
                    log.debug("Match event {} already processed. Skipping.", event.eventId());
                    return;
                }
                inboxEventRepository.save(new InboxEventEntity(event.eventId()));
                createChatUseCase.createChat(event.user1Id(), event.user2Id());
            });

        } catch (JsonProcessingException e) {
            log.error("Failed to parse MatchEvent JSON", e);
        } catch (Exception e) {
            log.error("Failed to process MatchEvent database transaction", e);
            throw e;
        }
    }
}