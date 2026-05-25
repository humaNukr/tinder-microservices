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
import java.util.List;

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

	@Override
	@Transactional
	public List<OutboxEvent> fetchAndLock(int batchSize) {
		List<OutboxEvent> events = outboxRepository.findAndLockUnprocessedEvents(batchSize);
		if (events.isEmpty()) {
			return events;
		}

		events.forEach(e -> e.setSent(true));
		return outboxRepository.saveAll(events);
	}

	@Override
	@Transactional
	public void markAsFailed(List<OutboxEvent> failedEvents) {
		if (failedEvents.isEmpty()) {
			return;
		}

		failedEvents.forEach(e -> e.setSent(false));
		outboxRepository.saveAll(failedEvents);
	}
}
