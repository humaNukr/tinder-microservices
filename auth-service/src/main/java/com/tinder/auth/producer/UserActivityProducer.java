package com.tinder.auth.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.auth.event.ActivityType;
import com.tinder.auth.event.UserActivityEvent;
import com.tinder.auth.properties.KafkaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActivityProducer {

	private final KafkaTemplate<String, String> kafkaTemplate;
	private final ObjectMapper objectMapper;
	private final KafkaProperties kafkaProperties;

	public void publishActivity(UUID userId, ActivityType type) {
		try {
			UserActivityEvent event = new UserActivityEvent(UUID.randomUUID(), userId, type, Instant.now());
			String payload = objectMapper.writeValueAsString(event);

			kafkaTemplate.send(kafkaProperties.userActivity(), userId.toString(), payload)
					.whenComplete((result, ex) -> {
						if (ex != null) {
							log.warn("Failed to send user activity event {} for user {}: {}", type, userId,
									ex.getMessage());
						} else {
							log.debug("Published activity {} for user {}", type, userId);
						}
					});
		} catch (Exception e) {
			log.error("Failed to serialize activity event {} for user {}", type, userId, e);
		}
	}
}
