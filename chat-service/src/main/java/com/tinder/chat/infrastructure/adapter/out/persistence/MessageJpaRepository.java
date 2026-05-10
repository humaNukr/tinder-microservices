package com.tinder.chat.infrastructure.adapter.out.persistence;

import com.tinder.chat.domain.enums.MessageStatus;
import com.tinder.chat.domain.model.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageJpaRepository extends JpaRepository<Message, Long> {

    @EntityGraph(attributePaths = {"reactions"})
    @Query("SELECT m FROM Message m WHERE m.id = :id")
    Optional<Message> findByIdWithReactions(@Param("id") Long id);

    List<Message> findByChatIdAndStatusNotOrderByIdDesc(UUID chatId, MessageStatus status, Pageable pageable);

    List<Message> findByChatIdAndStatusNotAndIdLessThanOrderByIdDesc(
            UUID chatId, MessageStatus status, Long cursor, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.content = :objectKey AND m.status = 'UPLOADING'")
    Optional<Message> findPendingMessageByObjectKey(@Param("objectKey") String objectKey);
}