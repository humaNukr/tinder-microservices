package com.tinder.auth.publisher;

import com.tinder.auth.entity.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaMessageBroker implements MessageBroker {

	private final KafkaTemplate<String, String> kafkaTemplate;

	@Override
	public CompletableFuture<OutboxEvent> send(OutboxEvent event) {
		return kafkaTemplate.send(event.getTopic(), String.valueOf(event.getId()), event.getPayload())
				.thenApply(sendResult -> (OutboxEvent) null).exceptionally(ex -> {
					log.error("Failed to send event {}: {}", event.getId(), ex.getMessage());
					return event;
				});
	}
}
