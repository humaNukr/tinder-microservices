package com.tinder.chat.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.chat.infrastructure.redis.RedisChatHandlers;
import com.tinder.chat.infrastructure.redis.RedisPresenceHandlers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisMessageListenerContainer redisContainer(
            RedisConnectionFactory connectionFactory,
            RedisChatHandlers chatHandlers,
            RedisPresenceHandlers presenceHandlers,
            RedisChatProperties chatProperties,
            RedisPresenceProperties presenceProperties
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        container.addMessageListener(
                createAdapter(chatHandlers, "handleChatMessage"),
                new ChannelTopic(chatProperties.channel())
        );
        container.addMessageListener(
                createAdapter(chatHandlers, "handleTypingEvent"),
                new ChannelTopic(chatProperties.typingChannel())
        );
        container.addMessageListener(
                createAdapter(chatHandlers, "handleReadReceipt"),
                new ChannelTopic(chatProperties.readReceiptChannel())
        );
        container.addMessageListener(
                createAdapter(presenceHandlers, "handlePresenceEvent"),
                new ChannelTopic(presenceProperties.channel())
        );

        container.addMessageListener(
                createAdapter(chatHandlers, "handleMessageDeletedEvent"),
                new ChannelTopic(chatProperties.deleteChannel())
        );

        container.addMessageListener(
                createAdapter(chatHandlers, "handleReactionEvent"),
                new ChannelTopic(chatProperties.reactionChannel())
        );

        return container;
    }


    private MessageListenerAdapter createAdapter(Object delegate, String methodName) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(delegate, methodName);
        adapter.afterPropertiesSet();
        return adapter;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper
    ) {
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        return template;
    }
}