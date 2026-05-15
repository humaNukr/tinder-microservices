package com.tinder.chat.infrastructure.adapter.out.scheduler;

import com.tinder.chat.infrastructure.adapter.out.persistence.inbox.InboxEventJpaRepository;
import com.tinder.chat.infrastructure.adapter.out.persistence.outbox.OutboxJpaRepository;
import com.tinder.chat.infrastructure.adapter.out.scheduler.inbox.InboxCleanupWorker;
import com.tinder.chat.infrastructure.adapter.out.scheduler.outbox.OutboxCleanupWorker;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CleanupWorkersTest {

    @Nested
    class InboxCleanupWorkerTest {
        @Mock
        private InboxEventJpaRepository inboxRepository;

        @InjectMocks
        private InboxCleanupWorker worker;

        @Test
        void cleanupInbox_CallsRepositoryWithCorrectThreshold() {
            when(inboxRepository.deleteOlderThan(any(Instant.class))).thenReturn(5);

            worker.cleanupInbox();

            ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
            verify(inboxRepository).deleteOlderThan(captor.capture());

            Instant capturedThreshold = captor.getValue();
            Instant expectedMax = Instant.now().minus(6, java.time.temporal.ChronoUnit.DAYS);
            Instant expectedMin = Instant.now().minus(8, java.time.temporal.ChronoUnit.DAYS);

            assertTrue(capturedThreshold.isBefore(expectedMax) && capturedThreshold.isAfter(expectedMin));
        }
    }

    @Nested
    class OutboxCleanupWorkerTest {
        @Mock
        private OutboxJpaRepository outboxRepository;

        @InjectMocks
        private OutboxCleanupWorker worker;

        @Test
        void cleanupOutbox_CallsRepositoryWithCorrectThreshold() {
            when(outboxRepository.deleteProcessedAndOlderThan(any(LocalDateTime.class))).thenReturn(10);

            worker.cleanupOutbox();

            ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(outboxRepository).deleteProcessedAndOlderThan(captor.capture());

            LocalDateTime capturedThreshold = captor.getValue();
            LocalDateTime expectedMax = LocalDateTime.now().minusDays(6);
            LocalDateTime expectedMin = LocalDateTime.now().minusDays(8);

            assertTrue(capturedThreshold.isBefore(expectedMax) && capturedThreshold.isAfter(expectedMin));
        }
    }
}