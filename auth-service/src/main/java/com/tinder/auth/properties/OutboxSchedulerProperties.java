package com.tinder.auth.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.outbox.scheduler")
public record OutboxSchedulerProperties(Integer pollInterval, Integer batchSize, Integer batchProcessingTime, // seconds
                                        Integer fixedDelay, // ms
                                        String cleanupCron // cron expression
) {
}
