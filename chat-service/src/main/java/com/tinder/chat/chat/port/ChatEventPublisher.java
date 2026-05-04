package com.tinder.chat.chat.port;

import com.tinder.chat.chat.dto.TypingEventDto;
import com.tinder.chat.message.dto.MessageEventDto;
import com.tinder.chat.chat.dto.ReadReceiptEventDto;

public interface ChatEventPublisher {
    void publishNewMessage(MessageEventDto eventDto);

    void publishTypingEvent(TypingEventDto eventDto);

    void publishReadReceipt(ReadReceiptEventDto eventDto);
}