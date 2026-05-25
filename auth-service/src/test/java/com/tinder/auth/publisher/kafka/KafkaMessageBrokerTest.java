package com.tinder.auth.publisher.kafka;

import com.tinder.auth.entity.OutboxEvent;
import com.tinder.auth.publisher.KafkaMessageBroker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaMessageBrokerTest {

	@Mock
	private KafkaTemplate<String, String> kafkaTemplate;

	@InjectMocks
	private KafkaMessageBroker messageBroker;

	@Test
	@DisplayName("Should return null when Kafka successfully sends the message")
	void send_Success_ReturnsNull() {
		OutboxEvent event = createTestEvent(1L, "test-topic", "test-payload");

		when(kafkaTemplate.send(eq("test-topic"), eq("1"), eq("test-payload")))
				.thenReturn(CompletableFuture.completedFuture(new SendResult<>(null, null)));

		CompletableFuture<OutboxEvent> result = messageBroker.send(event);

		assertNull(result.join());
	}

	@Test
	@DisplayName("Should return the original event when Kafka throws an exception")
	void send_Failure_ReturnsEventForRollback() {
		OutboxEvent event = createTestEvent(1L, "test-topic", "test-payload");

		CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
		failedFuture.completeExceptionally(new RuntimeException("Kafka timeout"));

		when(kafkaTemplate.send(eq("test-topic"), eq("1"), eq("test-payload"))).thenReturn(failedFuture);

		CompletableFuture<OutboxEvent> result = messageBroker.send(event);

		assertEquals(event, result.join());
	}

	private OutboxEvent createTestEvent(Long id, String topic, String payload) {
		OutboxEvent event = new OutboxEvent(topic, payload, LocalDateTime.now());
		ReflectionTestUtils.setField(event, "id", id);
		return event;
	}
}
