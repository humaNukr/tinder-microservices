package com.tinder.chat.chat.port;

import com.tinder.chat.message.dto.MessageEventDto;

public interface ChatEventSubscriber {
    void handleMessage(MessageEventDto eventDto);
}
