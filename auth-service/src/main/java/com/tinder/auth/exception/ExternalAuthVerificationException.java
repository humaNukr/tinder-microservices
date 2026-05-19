package com.tinder.auth.exception;

import lombok.Getter;

public class ExternalAuthVerificationException extends RuntimeException {
    @Getter
    private final ErrorType type;

    public ExternalAuthVerificationException(String message, ErrorType type, Throwable cause) {
        super(message, cause);
        this.type = type;
    }

    public enum ErrorType {
        NETWORK_ERROR, INVALID_TOKEN
    }
}
