package com.tinder.chat.infrastructure.adapter.out.event;

import com.tinder.chat.domain.event.MessageSavedEvent;
import com.tinder.chat.domain.model.Message;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SpringOutboxEventAdapterTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private SpringOutboxEventAdapter adapter;

    @Captor
    private ArgumentCaptor<MessageSavedEvent> eventCaptor;

    @Nested
    class PublishMessageSavedEvent {

        @Test
        void publishMessageSavedEvent_ValidInputs_PublishesEvent() {
            Message mockMessage = mock(Message.class);
            UUID recipientId = UUID.randomUUID();

            adapter.publishMessageSavedEvent(mockMessage, recipientId);

            verify(eventPublisher).publishEvent(eventCaptor.capture());
            MessageSavedEvent publishedEvent = eventCaptor.getValue();
            assertEquals(mockMessage, publishedEvent.savedMessage());
            assertEquals(recipientId, publishedEvent.recipientId());
        }
    }
}