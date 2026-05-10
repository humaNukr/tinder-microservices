package com.tinder.chat.application.port.in.activity;

import com.tinder.chat.shared.dto.activity.ReadReceiptRequest;

import java.util.UUID;

public interface ProcessReadReceiptUseCase {
    void processReadReceipt(UUID readerId, ReadReceiptRequest request);
}