package com.tinder.swipe.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "outbox.scheduler")
public record OutboxSchedulerProperties(
        int batchSize,
        int batchProcessingTime,
        long fixedDelay,
        String cleanupCron
) {
}
