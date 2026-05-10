package com.tinder.chat.application.port.out.notification;

import com.tinder.chat.shared.dto.event.MessageAckDto;
import com.tinder.chat.shared.dto.event.MessageDeletedEventDto;
import com.tinder.chat.shared.dto.event.MessageEditedEventDto;
import com.tinder.chat.shared.dto.event.MessageEventDto;
import com.tinder.chat.shared.dto.event.ReactionEventDto;
import com.tinder.chat.shared.dto.event.ReadReceiptEventDto;
import com.tinder.chat.shared.dto.event.TypingEventDto;
import com.tinder.chat.shared.dto.event.UserPresenceEvent;

import java.util.UUID;

public interface ClientNotificationPort {
    void sendAck(UUID userId, MessageAckDto ackDto);

    void sendNewMessage(MessageEventDto eventDto);

    void sendTypingEvent(TypingEventDto eventDto);

    void sendReadReceipt(ReadReceiptEventDto eventDto);

    void sendReaction(ReactionEventDto eventDto);

    void sendMessageDeleted(MessageDeletedEventDto eventDto);

    void sendMessageEdited(MessageEditedEventDto eventDto);

    void sendPresenceEvent(UserPresenceEvent event);
}