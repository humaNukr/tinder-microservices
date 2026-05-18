package com.tinder.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.auth.entity.OutboxEvent;
import com.tinder.auth.repository.OutboxRepository;
import com.tinder.auth.service.impl.OutboxServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxServiceImplTest {

	@Mock
	private OutboxRepository outboxRepository;

	@Mock
	private ObjectMapper objectMapper;

	@InjectMocks
	private OutboxServiceImpl outboxService;

	@Test
	@DisplayName("saveEvent() should serialize object and save OutboxEvent to DB")
	void saveEvent_ValidEvent_SavesToRepository() throws JsonProcessingException {
		String topic = "user-activity";
		Object eventPayload = Map.of("userId", "123");
		String expectedJson = "{\"userId\":\"123\"}";

		when(objectMapper.writeValueAsString(eventPayload)).thenReturn(expectedJson);

		outboxService.saveEvent(topic, eventPayload);

		ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
		verify(outboxRepository).save(eventCaptor.capture());

		OutboxEvent savedEvent = eventCaptor.getValue();

		assertAll(() -> assertEquals(topic, savedEvent.getTopic()),
				() -> assertEquals(expectedJson, savedEvent.getPayload()),
				() -> assertNotNull(savedEvent.getCreatedAt()), () -> assertEquals(false, savedEvent.getIsSent()));
	}

	@Test
	@DisplayName("saveEvent() should throw IllegalArgumentException when event is null")
	void saveEvent_NullEvent_ThrowsIllegalArgumentException() {
		assertThrows(IllegalArgumentException.class, () -> outboxService.saveEvent("topic", null));
		verifyNoInteractions(objectMapper, outboxRepository);
	}

	@Test
	@DisplayName("saveEvent() should wrap JsonProcessingException in RuntimeException")
	void saveEvent_JsonProcessingException_ThrowsRuntimeException() throws JsonProcessingException {
		Object eventPayload = new Object();
		when(objectMapper.writeValueAsString(eventPayload)).thenThrow(mock(JsonProcessingException.class));

		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> outboxService.saveEvent("topic", eventPayload));

		assertEquals("Failed to serialize outbox event payload", exception.getMessage());
		verifyNoInteractions(outboxRepository);
	}
}
