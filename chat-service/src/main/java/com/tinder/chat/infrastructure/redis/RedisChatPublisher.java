package com.tinder.chat.infrastructure.redis;

import com.tinder.chat.chat.dto.ReadReceiptEventDto;
import com.tinder.chat.chat.dto.TypingEventDto;
import com.tinder.chat.chat.port.ChatEventPublisher;
import com.tinder.chat.config.RedisChatProperties;
import com.tinder.chat.message.dto.MessageEventDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisChatPublisher implements ChatEventPublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisChatProperties redisProperties;

    @Override
    public void publishNewMessage(MessageEventDto eventDto) {
        redisTemplate.convertAndSend(redisProperties.channel(), eventDto);
    }

    @Override
    public void publishTypingEvent(TypingEventDto eventDto) {
        redisTemplate.convertAndSend(redisProperties.typingChannel(), eventDto);
    }

    @Override
    public void publishReadReceipt(ReadReceiptEventDto eventDto) {
        redisTemplate.convertAndSend(redisProperties.readReceiptChannel(), eventDto);
    }
}