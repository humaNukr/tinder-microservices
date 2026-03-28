package com.tinder.swipe.service.interfaces;

import com.tinder.swipe.event.MatchEvent;

public interface SwipeEventPublisher {
    void sendMatchEvent(MatchEvent matchEvent);
}
