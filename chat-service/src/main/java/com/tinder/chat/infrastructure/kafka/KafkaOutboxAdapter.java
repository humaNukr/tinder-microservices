package com.tinder.chat.infrastructure.kafka;

import com.tinder.chat.infrastructure.port.MessageBrokerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class KafkaOutboxAdapter implements MessageBrokerPort {
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public void sendEvent(String topic, UUID key, String payload) throws Exception {
        kafkaTemplate.send(topic, key.toString(), payload).get();
    }
}