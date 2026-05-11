package com.tinder.chat.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageReaction {

    private Long id;
    private Message message;
    private UUID userId;
    private String reaction;
    private Instant createdAt;
}