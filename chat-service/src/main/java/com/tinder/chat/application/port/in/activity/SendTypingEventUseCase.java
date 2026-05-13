package com.tinder.chat.application.port.in.activity;

import com.tinder.chat.shared.dto.event.TypingEventDto;

import java.util.UUID;

public interface SendTypingEventUseCase {
    void processTypingEvent(TypingEventDto requestDto);
}