package com.tinder.chat.domain.model;

import com.tinder.chat.domain.enums.MessageContentType;
import com.tinder.chat.domain.enums.MessageStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "messages", indexes = {
        @Index(name = "idx_messages_chat_id_id", columnList = "chat_id, id DESC")
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false)
    private UUID chatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_message_id")
    private Message parentMessage;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 20)
    private MessageContentType contentType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MessageStatus status;

    @Builder.Default
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MessageReaction> reactions = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "edited_at")
    private Instant editedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public void deleteBy(UUID requesterId, UUID contextChatId) {
        if (!this.senderId.equals(requesterId)) {
            throw new IllegalStateException("You can only delete your own messages");
        }
        if (!this.chatId.equals(contextChatId)) {
            throw new IllegalArgumentException("Message does not belong to this chat");
        }
        if (this.isDeleted()) {
            return;
        }

        this.deletedAt = Instant.now();
        this.content = "";
        this.contentType = MessageContentType.TEXT;
        this.status = MessageStatus.DELETED;
    }

    public void edit(String newContent) {
        if (this.isDeleted()) {
            throw new IllegalStateException("Cannot edit a deleted message");
        }

        if (this.contentType != MessageContentType.TEXT) {
            throw new IllegalStateException("Cannot edit non-text messages");
        }

        this.content = newContent;
        this.status = MessageStatus.EDITED;
        this.editedAt = Instant.now();
    }

    public void markAsSent() {
        if (this.status.ordinal() > MessageStatus.SENT.ordinal()) {
            throw new IllegalStateException("Cannot downgrade message status from " + this.status + " to SENT");
        }

        this.status = MessageStatus.SENT;
    }

    public void addReaction(MessageReaction reaction) {
        reactions.add(reaction);
        reaction.setMessage(this);
    }

    public void removeReaction(MessageReaction reaction) {
        reactions.remove(reaction);
        reaction.setMessage(null);
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}