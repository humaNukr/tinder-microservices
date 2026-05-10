package com.tinder.chat.infrastructure.adapter.out.scheduler.inbox;

import com.tinder.chat.infrastructure.adapter.out.persistence.inbox.InboxEventJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class InboxCleanupWorker {

    private final InboxEventJpaRepository inboxRepository;

    @Scheduled(cron = "${app.inbox.scheduler.cleanup-cron}")
    @Transactional
    public void cleanupInbox() {
        Instant threshold = Instant.now().minus(7, ChronoUnit.DAYS);

        int deletedCount = inboxRepository.deleteOlderThan(threshold);

        if (deletedCount > 0) {
            log.info("Cleaned up {} old inbox events", deletedCount);
        }
    }
}