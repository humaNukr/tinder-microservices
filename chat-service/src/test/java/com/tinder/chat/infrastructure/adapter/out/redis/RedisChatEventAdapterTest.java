package com.tinder.chat.infrastructure.adapter.out.redis;

import com.tinder.chat.infrastructure.config.properties.RedisChatProperties;
import com.tinder.chat.shared.dto.event.MessageEventDto;
import com.tinder.chat.shared.dto.event.TypingEventDto;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisChatEventAdapterTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private RedisChatProperties redisProperties;

    @InjectMocks
    private RedisChatEventAdapter adapter;

    @Nested
    class PublishEvents {

        @Test
        void publishNewMessage_SendsToCorrectChannel() {
            MessageEventDto dto = mock(MessageEventDto.class);
            when(redisProperties.channel()).thenReturn("msg-channel");

            adapter.publishNewMessage(dto);

            verify(redisTemplate).convertAndSend("msg-channel", dto);
        }

        @Test
        void publishTypingEvent_SendsToCorrectChannel() {
            TypingEventDto dto = mock(TypingEventDto.class);
            when(redisProperties.typingChannel()).thenReturn("typing-channel");

            adapter.publishTypingEvent(dto);

            verify(redisTemplate).convertAndSend("typing-channel", dto);
        }
    }
}