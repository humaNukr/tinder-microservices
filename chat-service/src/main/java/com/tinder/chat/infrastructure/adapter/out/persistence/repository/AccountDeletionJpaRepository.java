package com.tinder.chat.infrastructure.adapter.out.persistence.repository;

import com.tinder.chat.infrastructure.adapter.out.persistence.entity.ChatJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AccountDeletionJpaRepository extends JpaRepository<ChatJpaEntity, UUID> {

    @Query("SELECT c.id FROM ChatJpaEntity c WHERE c.user1Id = :userId OR c.user2Id = :userId")
    List<UUID> findChatIdsByUserId(@Param("userId") UUID userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            value =
                    """
                            DELETE FROM messages
                            WHERE chat_id IN (
                                SELECT id FROM chats
                                WHERE user1_id = :userId OR user2_id = :userId
                            )
                            """,
            nativeQuery = true)
    int deleteMessagesByUserId(@Param("userId") UUID userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            value = "DELETE FROM chats WHERE user1_id = :userId OR user2_id = :userId",
            nativeQuery = true)
    int deleteChatsByUserId(@Param("userId") UUID userId);
}
