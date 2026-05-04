package com.tinder.chat.chat.service;

import com.tinder.chat.chat.repository.ChatParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatParticipantServiceImpl implements ChatParticipantService {

    private final ChatParticipantRepository participantRepository;

    @Override
    @Transactional(readOnly = true)
    public Long getParticipantWatermark(UUID chatId, UUID userId) {
        return participantRepository.findLastReadMessageId(chatId, userId)
                .orElse(0L);
    }

    @Override
    @Transactional
    public int updateWatermark(UUID chatId, UUID userId, Long messageId) {
        return participantRepository.updateLastReadMessageIdIfGreater(chatId, userId, messageId);
    }
}