package com.tinder.auth.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.auth.entity.OutboxEvent;
import com.tinder.auth.repository.OutboxRepository;
import com.tinder.auth.service.interfaces.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxServiceImpl implements OutboxService {

	private final OutboxRepository outboxRepository;
	private final ObjectMapper objectMapper;

	@Override
	@Transactional
	public void saveEvent(String topic, Object event) {
		if (event == null) {
			throw new IllegalArgumentException("Event cannot be null");
		}

		try {
			String payloadJson = objectMapper.writeValueAsString(event);

			OutboxEvent outboxEvent = new OutboxEvent(topic, payloadJson, LocalDateTime.now());
			outboxRepository.save(outboxEvent);

			log.debug("Saved outbox event to DB for topic: {}", topic);

		} catch (JsonProcessingException e) {
			log.error("Failed to serialize outbox event payload for topic: {}", topic, e);
			throw new RuntimeException("Failed to serialize outbox event payload", e);
		}
	}
}
