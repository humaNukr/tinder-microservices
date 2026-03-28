package com.tinder.swipe.service.interfaces;

public interface OutboxService {
    void saveEvent(String topic, Object event);
}
