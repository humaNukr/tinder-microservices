package com.tinder.chat.util;

import com.tinder.chat.infrastructure.adapter.out.minio.MinioStorageAdapter;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@TestPropertySource(properties = {
        "app.redis.chat.channel=chat-channel",
        "app.redis.chat.typing-channel=chat-typing-channel",
        "app.redis.chat.read-receipt-channel=chat-read-receipt-channel",
        "app.redis.chat.reaction-channel=chat:reactions",
        "app.redis.chat.delete-channel=chat:deletions",
        "app.redis.chat.edit-channel=chat:edits",
        "app.redis.chat.key-prefix=chat:",
        "app.redis.chat.key-suffix=:participants",
        "app.redis.chat.ttl=7d",
        "app.redis.presence.channel=user-presence-channel",
        "app.redis.presence.session-key-prefix=user:sessions:",
        "app.redis.presence.grace-period-prefix=user:grace:",
        "app.redis.presence.grace-period-duration=15s",
        "app.kafka.topics.match-events=match-events",
        "app.kafka.topics.message-events=message-events",
        "app.kafka.topics.minio-chat-media-events=minio-chat-media-events",
        "app.kafka.topics.user-presence-events=user-presence-events",
        "app.kafka.topics.user-activity=user-activity-events-test",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer"
})
public abstract class IntegrationTestBase {

    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:3.7.0"));
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    static {
        postgres.start();
        kafka.start();
        redis.start();
    }

    @MockitoBean
    private MinioStorageAdapter minioStorageAdapter;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }
}