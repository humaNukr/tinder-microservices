package com.tinder.chat.chat.service;

import com.tinder.chat.chat.dto.ReadReceiptRequest;
import com.tinder.chat.chat.dto.TypingEventDto;

import java.util.UUID;

public interface ChatActivityService {

    void processReadReceipt(UUID readerId, ReadReceiptRequest request);

    void processTypingEvent(TypingEventDto requestDto, UUID senderId);
}
