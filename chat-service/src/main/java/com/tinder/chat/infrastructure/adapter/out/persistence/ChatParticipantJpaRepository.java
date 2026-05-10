package com.tinder.chat.infrastructure.adapter.out.persistence;

import com.tinder.chat.domain.model.ChatParticipant;
import com.tinder.chat.domain.model.ChatParticipantId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ChatParticipantJpaRepository extends JpaRepository<ChatParticipant, ChatParticipantId> {

    @Modifying(clearAutomatically = true)
    @Query("""
                UPDATE ChatParticipant cp 
                SET cp.lastReadMessageId = :messageId 
                WHERE cp.id.chatId = :chatId 
                  AND cp.id.userId = :userId 
                  AND cp.lastReadMessageId < :messageId
            """)
    int updateLastReadMessageIdIfGreater(
            @Param("chatId") UUID chatId,
            @Param("userId") UUID userId,
            @Param("messageId") Long messageId
    );

    @Query("SELECT cp.lastReadMessageId FROM ChatParticipant cp WHERE cp.id.chatId = :chatId AND cp.id.userId = :userId")
    Optional<Long> findLastReadMessageId(@Param("chatId") UUID chatId, @Param("userId") UUID userId);
}