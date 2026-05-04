package com.tinder.chat.chat.repository;

import com.tinder.chat.chat.model.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ChatRepository extends JpaRepository<Chat, UUID> {

    @Query("SELECT c.user1Id as user1Id, c.user2Id as user2Id FROM Chat c WHERE c.id = :chatId")
    Optional<ChatParticipantsProjection> findParticipantsById(@Param("chatId") UUID chatId);
}
