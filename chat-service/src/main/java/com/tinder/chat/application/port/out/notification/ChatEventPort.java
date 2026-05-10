package com.tinder.chat.application.port.out.notification;

import com.tinder.chat.shared.dto.event.MessageDeletedEventDto;
import com.tinder.chat.shared.dto.event.MessageEditedEventDto;
import com.tinder.chat.shared.dto.event.MessageEventDto;
import com.tinder.chat.shared.dto.event.ReactionEventDto;
import com.tinder.chat.shared.dto.event.ReadReceiptEventDto;
import com.tinder.chat.shared.dto.event.TypingEventDto;

public interface ChatEventPort {
    void publishNewMessage(MessageEventDto eventDto);

    void publishTypingEvent(TypingEventDto eventDto);

    void publishReadReceipt(ReadReceiptEventDto eventDto);

    void publishReaction(ReactionEventDto eventDto);

    void publishMessageDeleted(MessageDeletedEventDto eventDto);

    void publishMessageEdited(MessageEditedEventDto eventDto);
}