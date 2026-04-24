package com.tinder.auth.exception;

import com.tinder.auth.dto.error.ErrorResponseDto;
import jakarta.persistence.EntityNotFoundException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			@NonNull HttpHeaders headers, @NonNull HttpStatusCode status, @NonNull WebRequest request) {
		Map<String, String> errors = new LinkedHashMap<>();
		for (ObjectError error : ex.getBindingResult().getAllErrors()) {
			String fieldName = (error instanceof FieldError fe) ? fe.getField() : error.getObjectName();
			String errorMessage = error.getDefaultMessage();
			errors.put(fieldName, errorMessage);
		}

		return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", errors);
	}

	@ExceptionHandler(ExternalAuthVerificationException.class)
	public ResponseEntity<Object> handleGoogleTokenVerification(ExternalAuthVerificationException ex) {
		log.warn("External service token verification failed: {}", ex.getMessage());

		HttpStatus status;
		switch (ex.getType()) {
			case NETWORK_ERROR -> status = HttpStatus.SERVICE_UNAVAILABLE;
			case INVALID_TOKEN -> status = HttpStatus.UNAUTHORIZED;
			default -> status = HttpStatus.INTERNAL_SERVER_ERROR;
		}

		return buildResponse(status, ex.getMessage());
	}

	@ExceptionHandler(TooManyRequestsException.class)
	public ResponseEntity<Object> handleTooManyRequests(TooManyRequestsException ex) {
		return buildResponse(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests");
	}

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<Object> handleBadCredentials(BadCredentialsException ex) {
		return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
	}

	@ExceptionHandler(EntityNotFoundException.class)
	public ResponseEntity<Object> handleEntityNotFound(EntityNotFoundException ex) {
		log.warn("Resource not found: {}", ex.getMessage());
		return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Object> handleAllExceptions(Exception ex) {
		log.error("Unexpected error", ex);
		return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again later.");
	}

	private ResponseEntity<Object> buildResponse(HttpStatus status, String message, Map<String, String> errors) {
		ErrorResponseDto response = new ErrorResponseDto(LocalDateTime.now(), status.value(), status.getReasonPhrase(),
				message, errors);
		return new ResponseEntity<>(response, status);
	}

	private ResponseEntity<Object> buildResponse(HttpStatus status, String message) {
		return buildResponse(status, message, null);
	}
}
