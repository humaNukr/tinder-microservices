package com.tinder.swipe.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String MATCH_TOPIC = "match-events";

    @Bean
    public NewTopic matchEventsTopic() {
        return TopicBuilder.name(MATCH_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}