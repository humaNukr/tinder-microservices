package com.tinder.auth.exception;

public class ExternalAuthVerificationException extends RuntimeException {
	private final ErrorType type;

	public enum ErrorType {
		NETWORK_ERROR, INVALID_TOKEN
	}

	public ExternalAuthVerificationException(String message, ErrorType type, Throwable cause) {
		super(message, cause);
		this.type = type;
	}

	public ErrorType getType() {
		return type;
	}
}
