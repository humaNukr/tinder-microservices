package com.tinder.chat.chat.service;

import com.tinder.chat.infrastructure.inbox.InboxEvent;
import com.tinder.chat.infrastructure.inbox.InboxEventRepository;
import com.tinder.chat.infrastructure.kafka.contract.MatchEvent;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MatchEventProcessor {

    private final InboxEventRepository inboxEventRepository;
    private final ChatService chatService;

    @Transactional
    public void processMatchEvent(MatchEvent event) {
        if (inboxEventRepository.existsByEventId(event.eventId())) {
            return;
        }

        InboxEvent inboxEvent = new InboxEvent(event.eventId());
        inboxEventRepository.save(inboxEvent);

        chatService.createChat(event.user1Id(), event.user2Id());
    }
}