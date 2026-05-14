package com.tinder.chat.infrastructure.adapter.out.persistence.projections;

import java.util.UUID;

public interface ChatParticipantsProjection {
    UUID getUser1Id();

    UUID getUser2Id();
}