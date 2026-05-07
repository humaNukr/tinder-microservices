package com.tinder.chat.message.repository;

import com.tinder.chat.message.enums.MessageStatus;
import com.tinder.chat.message.model.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByChatIdAndStatusOrderByIdDesc(UUID chatId, MessageStatus status, Pageable pageable);

    List<Message> findByChatIdAndStatusAndIdLessThanOrderByIdDesc(UUID chatId, MessageStatus status, Long id, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.content = :objectKey AND m.status = 'UPLOADING'")
    Optional<Message> findPendingMessageByObjectKey(@Param("objectKey") String objectKey);
}