package com.tinder.swipe.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TestMatchEventConsumer {

    @KafkaListener(topics = "match-events", groupId = "test-swipe-group")
    public void consumeMatchEvent(ConsumerRecord<String, String> record) {
        log.info("✅ KAFKA CONSUMER RECEIVED EVENT | Key (Outbox ID): {} | Payload: {}",
                record.key(), record.value());
    }
}