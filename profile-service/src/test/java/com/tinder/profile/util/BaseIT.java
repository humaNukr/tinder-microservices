package com.tinder.profile.util;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
public abstract class BaseIT {

    protected static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    protected static final ConfluentKafkaContainer KAFKA =
            new ConfluentKafkaContainer("confluentinc/cp-kafka:7.4.3");

    @SuppressWarnings("resource")
    protected static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    protected static final MinIOContainer MINIO = new MinIOContainer("minio/minio:latest");

    static {
        MONGO.start();
        KAFKA.start();
        REDIS.start();
        MINIO.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", MONGO::getReplicaSetUrl);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("minio.url", MINIO::getS3URL);
        registry.add("minio.access-key", MINIO::getUserName);
        registry.add("minio.secret-key", MINIO::getPassword);
        registry.add("minio.bucket-name", () -> "tinder-media-test");
    }
}
