package com.tinder.chat.infrastructure.outbox;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@Getter
@Table(name = "outbox_events")
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "topic", nullable = false)
    private String topic;

    @Column(name = "payload", nullable = false)
    @Type(JsonType.class)
    private Object payload;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_sent", nullable = false)
    @Setter
    private Boolean isSent = false;

    public OutboxEvent(String topic, Object payload, LocalDateTime createdAt) {
        this.topic = topic;
        this.payload = payload;
        this.createdAt = createdAt;
    }
}