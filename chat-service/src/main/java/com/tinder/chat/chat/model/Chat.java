package com.tinder.chat.chat.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "chats")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class Chat implements Persistable<UUID> {
    @Id
    private UUID id;

    @Column(name = "user1_id", nullable = false)
    private UUID user1Id;

    @Column(name = "user2_id", nullable = false)
    private UUID user2Id;

    @Builder.Default
    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatParticipant> participants = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    public static Chat createNewChat(UUID userA, UUID userB) {
        boolean isALessThanB = userA.toString().compareTo(userB.toString()) < 0;

        UUID firstUser = isALessThanB ? userA : userB;
        UUID secondUser = isALessThanB ? userB : userA;

        return Chat.builder()
                .id(UUID.randomUUID())
                .user1Id(firstUser)
                .user2Id(secondUser)
                .build();
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    public void addParticipant(ChatParticipant participant) {
        this.participants.add(participant);
        participant.setChat(this);
    }
}
