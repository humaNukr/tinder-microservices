package com.tinder.swipe.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka.topics")
public record KafkaProperties (
        String swipe,
        String match
){
}
