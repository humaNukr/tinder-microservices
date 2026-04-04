package com.tinder.chat.messaging;

import com.tinder.chat.message.dto.MessageEventDto;

public interface ChatEventSubscriber {
    void handleMessage(MessageEventDto eventDto);
}
