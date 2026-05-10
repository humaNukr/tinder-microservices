package com.tinder.chat.infrastructure.adapter.out.redis;

import com.tinder.chat.application.port.out.notification.ChatEventPort;
import com.tinder.chat.infrastructure.config.properties.RedisChatProperties;
import com.tinder.chat.shared.dto.event.MessageDeletedEventDto;
import com.tinder.chat.shared.dto.event.MessageEditedEventDto;
import com.tinder.chat.shared.dto.event.MessageEventDto;
import com.tinder.chat.shared.dto.event.ReactionEventDto;
import com.tinder.chat.shared.dto.event.ReadReceiptEventDto;
import com.tinder.chat.shared.dto.event.TypingEventDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisChatEventAdapter implements ChatEventPort {

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

    @Override
    public void publishReaction(ReactionEventDto eventDto) {
        redisTemplate.convertAndSend(redisProperties.reactionChannel(), eventDto);
    }

    @Override
    public void publishMessageDeleted(MessageDeletedEventDto eventDto) {
        redisTemplate.convertAndSend(redisProperties.deleteChannel(), eventDto);
    }

    @Override
    public void publishMessageEdited(MessageEditedEventDto eventDto) {
        redisTemplate.convertAndSend(redisProperties.editChannel(), eventDto);
    }
}