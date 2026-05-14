package com.tinder.chat.application.port.in.activity;

import com.tinder.chat.shared.dto.event.TypingEventDto;

public interface SendTypingEventUseCase {
    void processTypingEvent(TypingEventDto requestDto);
}