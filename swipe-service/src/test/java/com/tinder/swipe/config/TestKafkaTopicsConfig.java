package com.tinder.swipe.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@Profile("test")
public class TestKafkaTopicsConfig {

    @Bean
    public NewTopic swipeEventsTestTopic() {
        return TopicBuilder.name("swipe-events-test").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic matchEventsTestTopic() {
        return TopicBuilder.name("match-events-test").partitions(1).replicas(1).build();
    }
}
