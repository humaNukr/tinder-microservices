package com.tinder.chat.infrastructure.adapter.out.persistence.repository;

import com.tinder.chat.infrastructure.adapter.out.persistence.entity.ChatParticipantId;
import com.tinder.chat.infrastructure.adapter.out.persistence.entity.ChatParticipantJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ChatParticipantJpaRepository extends JpaRepository<ChatParticipantJpaEntity, ChatParticipantId> {

    @Modifying(clearAutomatically = true)
    @Query("""
                UPDATE ChatParticipantJpaEntity cp 
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

    @Query("SELECT cp.lastReadMessageId FROM ChatParticipantJpaEntity cp WHERE cp.id.chatId = :chatId AND cp.id.userId = :userId")
    Optional<Long> findLastReadMessageId(@Param("chatId") UUID chatId, @Param("userId") UUID userId);
}