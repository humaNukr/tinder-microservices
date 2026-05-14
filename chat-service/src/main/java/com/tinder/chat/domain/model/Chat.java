package com.tinder.chat.domain.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Chat {

    private UUID id;
    private UUID user1Id;
    private UUID user2Id;

    @Builder.Default
    private List<ChatParticipant> participants = new ArrayList<>();

    private Instant createdAt;

    public static Chat createNewChat(UUID userA, UUID userB) {
        boolean isALessThanB = userA.toString().compareTo(userB.toString()) < 0;

        UUID firstUser = isALessThanB ? userA : userB;
        UUID secondUser = isALessThanB ? userB : userA;

        return Chat.builder()
                .id(UUID.randomUUID())
                .user1Id(firstUser)
                .user2Id(secondUser)
                .createdAt(Instant.now())
                .build();
    }

    public void addParticipant(ChatParticipant participant) {
        this.participants.add(participant);
        participant.setChat(this);
    }
}