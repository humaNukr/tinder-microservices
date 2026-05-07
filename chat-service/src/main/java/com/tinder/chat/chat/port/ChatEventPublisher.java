package com.tinder.chat.chat.port;

import com.tinder.chat.chat.dto.ReadReceiptEventDto;
import com.tinder.chat.chat.dto.TypingEventDto;
import com.tinder.chat.infrastructure.redis.contract.MessageDeletedEventDto;
import com.tinder.chat.message.dto.MessageEventDto;
import com.tinder.chat.message.dto.ReactionEventDto;

public interface ChatEventPublisher {
    void publishNewMessage(MessageEventDto eventDto);

    void publishTypingEvent(TypingEventDto eventDto);

    void publishReadReceipt(ReadReceiptEventDto eventDto);

    void publishReaction(ReactionEventDto eventDto);

    void publishMessageDeleted(MessageDeletedEventDto eventDto);
}