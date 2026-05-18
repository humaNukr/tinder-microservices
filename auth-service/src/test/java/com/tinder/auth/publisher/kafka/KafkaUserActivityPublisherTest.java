package com.tinder.auth.publisher.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.auth.event.ActivityType;
import com.tinder.auth.properties.KafkaProperties;
import com.tinder.auth.publisher.KafkaUserActivityPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaUserActivityPublisherTest {

	@Mock
	private KafkaTemplate<String, String> kafkaTemplate;

	@Mock
	private ObjectMapper objectMapper;

	@Mock
	private KafkaProperties kafkaProperties;

	@InjectMocks
	private KafkaUserActivityPublisher publisher;

	@Test
	void publishActivity_Success_SendsMessage() throws JsonProcessingException {
		UUID userId = UUID.randomUUID();
		when(kafkaProperties.userActivity()).thenReturn("user-activity-topic");
		when(objectMapper.writeValueAsString(any())).thenReturn("{\"data\":\"test\"}");
		when(kafkaTemplate.send(anyString(), anyString(), anyString()))
				.thenReturn(CompletableFuture.completedFuture(null));

		publisher.publishActivity(userId, ActivityType.LOGIN);

		verify(kafkaTemplate).send("user-activity-topic", userId.toString(), "{\"data\":\"test\"}");
	}

	@Test
	void publishActivity_SerializationFails_CatchesExceptionAndDoesNotSend() throws JsonProcessingException {
		UUID userId = UUID.randomUUID();
		when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("Serialization failed"));

		publisher.publishActivity(userId, ActivityType.LOGIN);

		verifyNoInteractions(kafkaTemplate);
	}
}
