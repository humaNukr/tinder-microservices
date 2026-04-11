package com.tinder.notification.repository;

import com.tinder.notification.entity.InboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InboxEventRepository extends JpaRepository<InboxEvent, UUID> {
    boolean existsByEventId(UUID uuid);
}
