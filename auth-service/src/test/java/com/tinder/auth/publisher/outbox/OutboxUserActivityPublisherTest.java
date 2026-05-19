package com.tinder.auth.publisher.outbox;

import com.tinder.auth.event.ActivityType;
import com.tinder.auth.event.UserActivityEvent;
import com.tinder.auth.properties.KafkaProperties;
import com.tinder.auth.publisher.OutboxUserActivityPublisher;
import com.tinder.auth.service.interfaces.OutboxService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxUserActivityPublisherTest {

    @Mock
    private OutboxService outboxService;

    @Mock
    private KafkaProperties kafkaProperties;

    @InjectMocks
    private OutboxUserActivityPublisher publisher;

    @Captor
    private ArgumentCaptor<UserActivityEvent> eventCaptor;

    @Test
    @DisplayName("Should successfully create UserActivityEvent and save it to OutboxService")
    void publishActivity_Success_DelegatesToOutbox() {
        UUID userId = UUID.randomUUID();
        when(kafkaProperties.userActivity()).thenReturn("user-activity-topic");

        publisher.publishActivity(userId, ActivityType.LOGIN);

        verify(outboxService).saveEvent(eq("user-activity-topic"), eventCaptor.capture());

        UserActivityEvent capturedEvent = eventCaptor.getValue();

        assertAll(
                () -> assertNotNull(capturedEvent.eventId(), "Event ID should be generated"),
                () -> assertNotNull(capturedEvent.timestamp(), "Timestamp should be generated"),
                () -> assertEquals(userId, capturedEvent.userId(), "User ID should match the input"),
                () -> assertEquals(ActivityType.LOGIN, capturedEvent.type(), "Activity type should match")
        );
    }
}