package com.tinder.chat.infrastructure.adapter.out.persistence.repository;

import com.tinder.chat.domain.enums.MessageStatus;
import com.tinder.chat.infrastructure.adapter.out.persistence.entity.MessageJpaEntity;
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
public interface MessageJpaRepository extends JpaRepository<MessageJpaEntity, Long> {

    @EntityGraph(attributePaths = {"reactions"})
    @Query("SELECT m FROM MessageJpaEntity m WHERE m.id = :id")
    Optional<MessageJpaEntity> findByIdWithReactions(@Param("id") Long id);

    List<MessageJpaEntity> findByChatIdAndStatusNotOrderByIdDesc(UUID chatId, MessageStatus status, Pageable pageable);

    List<MessageJpaEntity> findByChatIdAndStatusNotAndIdLessThanOrderByIdDesc(
            UUID chatId, MessageStatus status, Long cursor, Pageable pageable);

    @Query("SELECT m FROM MessageJpaEntity m WHERE m.content = :objectKey AND m.status = 'UPLOADING'")
    Optional<MessageJpaEntity> findPendingMessageByObjectKey(@Param("objectKey") String objectKey);
}