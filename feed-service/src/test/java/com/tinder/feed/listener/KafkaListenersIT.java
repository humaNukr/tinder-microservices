package com.tinder.feed.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.feed.event.ActivityType;
import com.tinder.feed.event.SwipeCreatedEvent;
import com.tinder.feed.event.UserActivityEvent;
import com.tinder.feed.properties.ProfileServiceProperties;
import com.tinder.feed.util.BaseIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Kafka Listeners — Integration Tests")
class KafkaListenersIT extends BaseIT {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ProfileServiceProperties profileProps;

    @Value("${app.kafka.topics.user-activity}")
    private String userActivityTopic;

    @Value("${app.kafka.topics.swipe-events}")
    private String swipeEventsTopic;

    private UUID userId;
    private UUID candidate1;
    private UUID candidate2;

    @BeforeEach
    void setUp() throws Exception {
        userId = UUID.randomUUID();
        candidate1 = UUID.randomUUID();
        candidate2 = UUID.randomUUID();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        wireMock.resetAll();
        stubFor(get(urlPathEqualTo(profileProps.candidatesPath()))
                .willReturn(okJson(objectMapper.writeValueAsString(List.of(candidate1, candidate2)))));
    }

    @Test
    @DisplayName("LOGIN event warms deck async")
    void loginEvent_TriggersAsyncGeneration() throws Exception {
        UserActivityEvent event = new UserActivityEvent(
                UUID.randomUUID(),
                userId,
                ActivityType.LOGIN,
                Instant.now()
        );

        kafkaTemplate.send(userActivityTopic, userId.toString(), objectMapper.writeValueAsString(event)).get();

        await().atMost(Duration.ofSeconds(15)).pollInterval(Duration.ofMillis(200)).untilAsserted(() -> {
            verify(getRequestedFor(urlPathEqualTo(profileProps.candidatesPath())));
            Long size = redisTemplate.opsForList().size("user:" + userId + ":deck");
            assertTrue(size != null && size == 2, "Deck має бути згенерований");
        });
    }

    @Test
    @DisplayName("LOCATION_UPDATE event deletes existing deck and triggers async generation")
    void locationUpdateEvent_ClearsDeckAndGenerates() throws Exception {
        String deckKey = "user:" + userId + ":deck";
        redisTemplate.opsForList().rightPush(deckKey, UUID.randomUUID().toString());

        UserActivityEvent event = new UserActivityEvent(
                UUID.randomUUID(),
                userId,
                ActivityType.LOCATION_UPDATE,
                Instant.now()
        );

        kafkaTemplate.send(userActivityTopic, userId.toString(), objectMapper.writeValueAsString(event)).get();

        await().atMost(Duration.ofSeconds(15)).pollInterval(Duration.ofMillis(200)).untilAsserted(() -> {
            verify(getRequestedFor(urlPathEqualTo(profileProps.candidatesPath())));
            Long size = redisTemplate.opsForList().size(deckKey);
            assertTrue(size != null && size == 2, "Deck має бути перегенерований");
        });
    }

    @Test
    @DisplayName("DELETE_ACCOUNT clears deck, history and profile cache")
    void deleteAccount_ClearsRedisKeys() throws Exception {
        String deckKey = "user:" + userId + ":deck";
        String historyKey = "user:" + userId + ":history";
        String profileKey = "user:" + userId + ":profile";

        redisTemplate.opsForList().rightPush(deckKey, candidate1.toString());
        redisTemplate.opsForSet().add(historyKey, candidate2.toString());
        redisTemplate.opsForValue().set(profileKey, "{}");

        UserActivityEvent event = new UserActivityEvent(
                UUID.randomUUID(), userId, ActivityType.DELETE_ACCOUNT, Instant.now());
        kafkaTemplate.send(userActivityTopic, userId.toString(), objectMapper.writeValueAsString(event)).get();

        await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(200)).untilAsserted(() -> {
            assertTrue(Boolean.FALSE.equals(redisTemplate.hasKey(deckKey)));
            assertTrue(Boolean.FALSE.equals(redisTemplate.hasKey(historyKey)));
            assertTrue(Boolean.FALSE.equals(redisTemplate.hasKey(profileKey)));
        });
    }

    @Test
    @DisplayName("Consumes swipe and saves to Redis history")
    void swipeEvent_SavesToHistorySet() throws Exception {
        UUID swipedId = UUID.randomUUID();

        SwipeCreatedEvent event = new SwipeCreatedEvent(
                userId,
                swipedId,
                true,
                Instant.now()
        );

        kafkaTemplate.send(swipeEventsTopic, userId.toString(), objectMapper.writeValueAsString(event)).get();

        String historyKey = "user:" + userId + ":history";

        await().atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    Boolean isMember = redisTemplate.opsForSet().isMember(historyKey, swipedId.toString());
                    assertTrue(Boolean.TRUE.equals(isMember),
                            "Swiped user ID " + swipedId + " should be in Redis history set under key " + historyKey);
                });
    }
}
