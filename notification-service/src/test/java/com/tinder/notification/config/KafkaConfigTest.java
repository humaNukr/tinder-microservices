package com.tinder.notification.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

@DisplayName("KafkaConfig")
class KafkaConfigTest {

    @Test
    @DisplayName("creates DefaultErrorHandler with dead-letter recoverer")
    void kafkaErrorHandler_CreatesHandler() {
        KafkaConfig config = new KafkaConfig();
        @SuppressWarnings("unchecked")
        KafkaTemplate<Object, Object> template = mock(KafkaTemplate.class);

        DefaultErrorHandler errorHandler = config.kafkaErrorHandler(template);

        assertNotNull(errorHandler);
    }
}
