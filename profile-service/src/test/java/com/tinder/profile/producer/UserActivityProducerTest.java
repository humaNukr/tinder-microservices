package com.tinder.profile.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tinder.profile.event.ActivityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserActivityProducer")
class UserActivityProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private UserActivityProducer producer;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        producer = new UserActivityProducer(kafkaTemplate, objectMapper);
        ReflectionTestUtils.setField(producer, "activityTopic", "user-activity-events-test");
    }

    @Test
    @DisplayName("publishes JSON activity event to kafka topic")
    void publishActivity_SendsJsonPayload() {
        UUID userId = UUID.randomUUID();
        when(kafkaTemplate.send(eq("user-activity-events-test"), eq(userId.toString()), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        producer.publishActivity(userId, ActivityType.LOCATION_UPDATE);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("user-activity-events-test"), eq(userId.toString()), captor.capture());

        assertTrue(captor.getValue().contains(userId.toString()));
        assertTrue(captor.getValue().contains("LOCATION_UPDATE"));
    }
}
