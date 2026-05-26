package com.tinder.swipe.service.impl;

import com.tinder.swipe.entity.OutboxEvent;
import com.tinder.swipe.event.SwipeCreatedEvent;
import com.tinder.swipe.repository.OutboxRepository;
import com.tinder.swipe.util.SwipeTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxServiceImpl")
class OutboxServiceImplTest {

    @Mock
    private OutboxRepository outboxRepository;

    @InjectMocks
    private OutboxServiceImpl outboxService;

    @Test
    @DisplayName("saveEvent() persists outbox row with topic and payload")
    void saveEvent_ValidEvent_SavesToRepository() {
        String topic = "swipe-events-test";
        SwipeCreatedEvent payload = SwipeTestFixtures.swipeCreatedEvent(
                SwipeTestFixtures.USER_ONE, SwipeTestFixtures.USER_TWO, true);

        outboxService.saveEvent(topic, payload);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(captor.capture());
        OutboxEvent saved = captor.getValue();

        assertAll(
                () -> assertEquals(topic, saved.getTopic()),
                () -> assertEquals(payload, saved.getPayload()),
                () -> assertNotNull(saved.getCreatedAt()),
                () -> assertFalse(saved.isSent()));
    }

    @Test
    @DisplayName("saveEvent() throws when event is null")
    void saveEvent_NullEvent_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> outboxService.saveEvent("topic", null));
        verifyNoInteractions(outboxRepository);
    }
}
