package com.tinder.chat.infrastructure.adapter.out.persistence.inbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inbox_events")
@NoArgsConstructor
public class InboxEventEntity {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant processedAt;

    public InboxEventEntity(UUID eventId) {
        this.eventId = eventId;
    }
}