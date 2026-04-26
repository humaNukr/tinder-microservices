package com.tinder.auth.scheduler;

import com.tinder.auth.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxCleanupWorker {

	private final OutboxRepository outboxRepository;

	@Scheduled(cron = "${outbox.scheduler.cleanup-cron}")
	@Transactional
	public void cleanupOutbox() {
		LocalDateTime threshold = LocalDateTime.now().minusDays(7);

		int deletedCount = outboxRepository.deleteProcessedAndOlderThan(threshold);

		if (deletedCount > 0) {
			log.info("Cleaned up {} old processed outbox events", deletedCount);
		}
	}
}
