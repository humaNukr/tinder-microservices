package com.tinder.profile.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.profile.event.UserPresenceEvent;
import com.tinder.profile.service.interfaces.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserPresenceListener {

    private final ProfileService profileService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${app.kafka.topics.user-presence}",
            groupId = "${spring.kafka.consumer.group-id}",
            concurrency = "3"
    )
    public void handlePresenceEvent(String payload) {
        try {
            UserPresenceEvent event = objectMapper.readValue(payload, UserPresenceEvent.class);
            log.debug("Received presence event: user={}, online={}", event.userId(), event.isOnline());

            if (!event.isOnline()) {
                profileService.updateLastSeen(event.userId(), event.timestamp());
            }
        } catch (Exception e) {
            log.error("Failed to process user presence event: {}", payload, e);
        }
    }
}