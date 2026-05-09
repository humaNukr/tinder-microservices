package com.tinder.chat.chat.port;

import com.tinder.chat.chat.dto.ReadReceiptEventDto;
import com.tinder.chat.chat.dto.TypingEventDto;
import com.tinder.chat.infrastructure.redis.contract.MessageDeletedEventDto;
import com.tinder.chat.message.dto.MessageAckDto;
import com.tinder.chat.message.dto.MessageEditedEventDto;
import com.tinder.chat.message.dto.MessageEventDto;
import com.tinder.chat.message.dto.ReactionEventDto;

import java.util.UUID;

public interface ClientNotificationPort {
    void sendAck(UUID userId, MessageAckDto ackDto);

    void sendNewMessage(MessageEventDto eventDto);

    void sendTypingEvent(TypingEventDto eventDto);

    void sendReadReceipt(ReadReceiptEventDto eventDto);

    void sendReaction(ReactionEventDto eventDto);

    void sendMessageDeleted(MessageDeletedEventDto eventDto);

    void sendMessageEdited(MessageEditedEventDto eventDto);
}