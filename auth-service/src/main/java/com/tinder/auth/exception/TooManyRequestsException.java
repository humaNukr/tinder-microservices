package com.tinder.auth.exception;

public class TooManyRequestsException extends RuntimeException {
	public TooManyRequestsException(String message) {
		super(message);
	}
}
