package com.tinder.chat.chat.repository;

import java.util.UUID;

public interface ChatParticipantsProjection {
    UUID getUser1Id();

    UUID getUser2Id();
}