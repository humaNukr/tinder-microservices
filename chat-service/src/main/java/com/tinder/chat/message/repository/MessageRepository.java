package com.tinder.chat.message.repository;

import com.tinder.chat.message.enums.MessageStatus;
import com.tinder.chat.message.model.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByChatIdOrderByIdDesc(UUID chatId, Pageable pageable);

    List<Message> findByChatIdAndIdLessThanOrderByIdDesc(UUID chatId, Long lastMessageId, Pageable pageable);

    Optional<Message> findByContentAndStatus(String content, MessageStatus status);
}