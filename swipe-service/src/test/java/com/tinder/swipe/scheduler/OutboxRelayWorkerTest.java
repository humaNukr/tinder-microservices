package com.tinder.swipe.scheduler;

import com.tinder.swipe.entity.OutboxEvent;
import com.tinder.swipe.properties.OutboxSchedulerProperties;
import com.tinder.swipe.service.impl.OutboxRelayTxHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxRelayWorker")
class OutboxRelayWorkerTest {

    @Mock
    private OutboxRelayTxHelper outboxRelayTxHelper;
    @Mock
    private OutboxSchedulerProperties outboxSchedulerProperties;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private OutboxRelayWorker outboxRelayWorker;

    @Test
    @DisplayName("processOutboxEvents() does nothing when queue is empty")
    void processOutboxEvents_NoEvents_SkipsKafkaAndMarkAsSent() {
        when(outboxSchedulerProperties.batchSize()).thenReturn(10);
        when(outboxRelayTxHelper.findAndLockUnprocessed(10)).thenReturn(List.of());

        outboxRelayWorker.processOutboxEvents();

        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
        verify(outboxRelayTxHelper, never()).markAsSent(anyList());
    }

    @Test
    @DisplayName("processOutboxEvents() marks only successfully sent events")
    void processOutboxEvents_PartialFailure_MarksOnlySuccessful() {
        OutboxEvent successEvent = new OutboxEvent("topic-ok", "{}", LocalDateTime.now());
        OutboxEvent failEvent = new OutboxEvent("topic-fail", "{}", LocalDateTime.now());

        when(outboxSchedulerProperties.batchSize()).thenReturn(10);
        when(outboxSchedulerProperties.batchProcessingTime()).thenReturn(2);
        when(outboxRelayTxHelper.findAndLockUnprocessed(10)).thenReturn(List.of(successEvent, failEvent));

        when(kafkaTemplate.send(eq("topic-ok"), anyString(), eq("{}")))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
        when(kafkaTemplate.send(eq("topic-fail"), anyString(), eq("{}")))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker down")));

        outboxRelayWorker.processOutboxEvents();

        verify(outboxRelayTxHelper).markAsSent(List.of(successEvent));
    }
}
