package com.tinder.chat.application.port.out.common;

import java.time.Duration;

public interface IdempotencyPort {

    boolean tryAcquire(String idempotencyKey, Duration ttl);

    void complete(String idempotencyKey, String resultValue, Duration ttl);

    String getResult(String idempotencyKey);
}