package com.tinder.chat.chat.port;

import java.time.Duration;

public interface IdempotencyPort {

    boolean tryAcquire(String idempotencyKey, Duration ttl);

    void complete(String idempotencyKey, String resultValue, Duration ttl);

    String getResult(String idempotencyKey);
}