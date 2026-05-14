package com.tinder.chat.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class ChatParticipant {

    private Chat chat;
    private UUID userId;
    private Long lastReadMessageId;

    public ChatParticipant(Chat chat, UUID userId) {
        this.chat = chat;
        this.userId = userId;
        this.lastReadMessageId = 0L;
    }
}