package com.tinder.chat.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "chat_participants")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatParticipantJpaEntity {

    @EmbeddedId
    @Builder.Default
    private ChatParticipantId id = new ChatParticipantId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("chatId")
    @JoinColumn(name = "chat_id")
    private ChatJpaEntity chat;

    @Column(name = "last_read_message_id")
    @Builder.Default
    private Long lastReadMessageId = 0L;
}