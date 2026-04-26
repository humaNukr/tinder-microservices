package com.tinder.profile.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document(collection = "inbox_events")
@Getter
@NoArgsConstructor
public class InboxEvent {

    @Id
    private UUID eventId;

    @Indexed(expireAfter = "7d")
    private Instant processedAt;

    public InboxEvent(UUID eventId) {
        this.eventId = eventId;
        this.processedAt = Instant.now();
    }
}