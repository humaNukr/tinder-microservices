package com.tinder.auth.scheduler;

import com.tinder.auth.entity.OutboxEvent;
import com.tinder.auth.repository.OutboxRepository;
import com.tinder.auth.util.BaseIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OutboxCleanupWorkerIT extends BaseIT {

	@Autowired
	private OutboxCleanupWorker outboxCleanupWorker;

	@Autowired
	private OutboxRepository outboxRepository;

	@BeforeEach
	void setUp() {
		outboxRepository.deleteAll();
	}

	@Test
	void cleanupOutbox_DeletesOnlyProcessedAndOldEvents() {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime oldDate = now.minusDays(10);

		OutboxEvent oldProcessed = new OutboxEvent("topic-1", "{}", oldDate);
		oldProcessed.setIsSent(true);

		OutboxEvent oldUnprocessed = new OutboxEvent("topic-2", "{}", oldDate);
		oldUnprocessed.setIsSent(false);

		OutboxEvent newProcessed = new OutboxEvent("topic-3", "{}", now);
		newProcessed.setIsSent(true);

		OutboxEvent newUnprocessed = new OutboxEvent("topic-4", "{}", now);
		newUnprocessed.setIsSent(false);

		outboxRepository.saveAll(List.of(oldProcessed, oldUnprocessed, newProcessed, newUnprocessed));

		outboxCleanupWorker.cleanupOutbox();

		assertEquals(3, outboxRepository.count());
	}
}
