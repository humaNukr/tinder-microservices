package com.tinder.profile.repository;

import com.tinder.profile.domain.InboxEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface InboxEventRepository extends MongoRepository<InboxEvent, UUID> {
    boolean existsByEventId(UUID uuid);
}