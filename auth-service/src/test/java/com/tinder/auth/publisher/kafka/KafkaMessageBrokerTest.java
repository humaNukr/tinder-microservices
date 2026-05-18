package com.tinder.auth.publisher.kafka;

import com.tinder.auth.publisher.KafkaMessageBroker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaMessageBrokerTest {

	@Mock
	private KafkaTemplate<String, String> kafkaTemplate;

	@InjectMocks
	private KafkaMessageBroker messageBroker;

	@Test
    void send_Success_ReturnsTrue() {
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(new SendResult<>(null, null)));

        CompletableFuture<Boolean> result = messageBroker.send("topic", "key", "payload");

        assertTrue(result.join());
    }

	@Test
	void send_Failure_ReturnsFalse() {
		CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
		failedFuture.completeExceptionally(new RuntimeException("Kafka timeout"));

		when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(failedFuture);

		CompletableFuture<Boolean> result = messageBroker.send("topic", "key", "payload");

		assertFalse(result.join());
	}
}
