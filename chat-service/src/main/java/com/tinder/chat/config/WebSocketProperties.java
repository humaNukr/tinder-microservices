package com.tinder.chat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.websocket")
public record WebSocketProperties(
        String appDestinationPrefix,
        String userDestinationPrefix,
        String chatEndpoint,
        String queueMessages,
        String queueAcks,
        String queueReadReceipts,
        String queueMessageReactions,
        String queueMessageDeletions,
        String topicUsersPrefix,
        String topicUsersPresenceSuffix
) {
}