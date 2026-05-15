package com.tinder.chat.infrastructure.adapter.out.persistence.repository;

import com.tinder.chat.infrastructure.adapter.out.persistence.entity.ChatJpaEntity;
import com.tinder.chat.infrastructure.adapter.out.persistence.projections.ChatParticipantsProjection;
import com.tinder.chat.infrastructure.adapter.out.persistence.projections.ChatPreviewProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatJpaRepository extends JpaRepository<ChatJpaEntity, UUID> {

    @Query("SELECT c.user1Id as user1Id, c.user2Id as user2Id FROM ChatJpaEntity c WHERE c.id = :chatId")
    Optional<ChatParticipantsProjection> findParticipantsById(@Param("chatId") UUID chatId);

    @Query(nativeQuery = true, value = """
                SELECT c.id as chatId,
                       CASE WHEN c.user1_id = :userId THEN c.user2_id ELSE c.user1_id END as partnerId,
                       m.content as lastMessageContent,
                       m.content_type as lastMessageType,
                       m.created_at as lastMessageCreatedAt,
                       m.sender_id as lastMessageSenderId,
                       (
                           SELECT COUNT(*)
                           FROM messages u
                           WHERE u.chat_id = c.id
                             AND u.status != 'UPLOADING'
                             AND u.sender_id != :userId
                             AND u.id > (
                                 SELECT last_read_message_id
                                 FROM chat_participants cp
                                 WHERE cp.chat_id = c.id AND cp.user_id = :userId
                             )
                       ) as unreadCount
                FROM chats c
                LEFT JOIN LATERAL (
                    SELECT content, content_type, created_at, sender_id
                    FROM messages
                    WHERE chat_id = c.id AND status != 'UPLOADING'
                    ORDER BY id DESC LIMIT 1
                ) m ON true
                WHERE c.user1_id = :userId OR c.user2_id = :userId
                ORDER BY m.created_at DESC NULLS LAST
                LIMIT :limit OFFSET :offset
            """)
    List<ChatPreviewProjection> findChatPreviewsByUserId(
            @Param("userId") UUID userId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );
}