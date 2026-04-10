package com.tinder.chat.chat.repository;

import com.tinder.chat.chat.model.ChatParticipant;
import com.tinder.chat.chat.model.ChatParticipantId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, ChatParticipantId> {
}
