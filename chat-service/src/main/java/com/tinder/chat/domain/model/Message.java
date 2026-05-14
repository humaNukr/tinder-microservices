package com.tinder.chat.domain.model;

import com.tinder.chat.domain.enums.MessageContentType;
import com.tinder.chat.domain.enums.MessageStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Message {

    private Long id;
    private UUID chatId;
    private Message parentMessage;
    private UUID senderId;
    private MessageContentType contentType;
    private String content;
    private MessageStatus status;

    @Builder.Default
    private List<MessageReaction> reactions = new ArrayList<>();

    private Instant createdAt;
    private Instant editedAt;
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