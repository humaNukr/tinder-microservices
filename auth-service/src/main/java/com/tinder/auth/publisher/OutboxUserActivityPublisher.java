package com.tinder.auth.publisher;

import com.tinder.auth.event.ActivityType;
import com.tinder.auth.event.UserActivityEvent;
import com.tinder.auth.properties.KafkaProperties;
import com.tinder.auth.service.interfaces.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxUserActivityPublisher implements UserActivityPublisher {

    private final OutboxService outboxService;
    private final KafkaProperties kafkaProperties;

    @Override
    public void publishActivity(UUID userId, ActivityType type) {
        UserActivityEvent event = new UserActivityEvent(UUID.randomUUID(), userId, type, Instant.now());

        outboxService.saveEvent(kafkaProperties.userActivity(), event);

        log.debug("Saved user activity {} for user {} to outbox", type, userId);
    }
}