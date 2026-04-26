package com.tinder.auth.service.impl;

import com.tinder.auth.entity.OutboxEvent;
import com.tinder.auth.repository.OutboxRepository;
import com.tinder.auth.service.interfaces.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OutboxServiceImpl implements OutboxService {
	private final OutboxRepository outboxRepository;

	@Override
	@Transactional
	public void saveEvent(String topic, Object event) {
		if (event == null) {
			throw new IllegalArgumentException("Event cannot be null");
		}

		OutboxEvent outboxEvent = new OutboxEvent(topic, event, LocalDateTime.now());
		outboxRepository.save(outboxEvent);
	}
}
