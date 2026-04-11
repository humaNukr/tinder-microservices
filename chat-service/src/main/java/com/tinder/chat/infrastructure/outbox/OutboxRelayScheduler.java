package com.tinder.chat.infrastructure.outbox;

import com.tinder.chat.infrastructure.port.MessageBrokerPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelayScheduler {

    private final OutboxEventRepository outboxEventRepository;
    private final MessageBrokerPort messageBroker;

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval}")
    public void processOutbox() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByProcessedFalse();

        for (OutboxEvent event : pendingEvents) {
            try {
                messageBroker.sendEvent(event.getTopic(), event.getId(), event.getPayload());
                outboxEventRepository.markAsProcessed(event.getId());

            } catch (Exception e) {
                log.error("Failed to publish outbox event {}. Halting batch processing.", event.getId(), e);
                break;
            }
        }
    }
}