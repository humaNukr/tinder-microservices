package com.tinder.chat.chat.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "chat_participants")
@Getter
@Setter
@NoArgsConstructor
public class ChatParticipant {

    @EmbeddedId
    private ChatParticipantId id = new ChatParticipantId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("chatId")
    @JoinColumn(name = "chat_id")
    private Chat chat;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId = 0L;

    public ChatParticipant(Chat chat, UUID userId) {
        this.chat = chat;
        this.id.setChatId(chat.getId());
        this.id.setUserId(userId);
    }

    public UUID getUserId() {
        return this.id.getUserId();
    }
}
