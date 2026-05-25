package com.tinder.auth.exception;

import com.tinder.auth.dto.error.ErrorResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

	private GlobalExceptionHandler globalExceptionHandler;

	@Mock
	private WebRequest webRequest;

	@BeforeEach
	void setUp() {
		globalExceptionHandler = new GlobalExceptionHandler();
	}

	@Test
	@DisplayName("handleMethodArgumentNotValid() maps errors correctly to details field")
	void handleMethodArgumentNotValid_Returns400WithDetailsMap() {
		MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
		BindingResult bindingResult = mock(BindingResult.class);

		FieldError fieldError = new FieldError("authDto", "email", "must not be blank");
		ObjectError objectError = new ObjectError("authDto", "Invalid request structure");

		when(ex.getBindingResult()).thenReturn(bindingResult);
		when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError, objectError));

		ResponseEntity<Object> responseEntity = globalExceptionHandler.handleMethodArgumentNotValid(ex,
				new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest);

		ErrorResponseDto body = (ErrorResponseDto) responseEntity.getBody();

		assertAll(() -> assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode()), () -> assertNotNull(body),
				() -> assertEquals(400, body.status()), () -> assertEquals("Validation failed", body.message()),
				() -> assertNotNull(body.details()), () -> assertEquals(2, body.details().size()),
				() -> assertEquals("must not be blank", body.details().get("email")),
				() -> assertEquals("Invalid request structure", body.details().get("authDto")));
	}

	@Test
	void handleExternalAuthVerification_NetworkError_Returns503() {
		ExternalAuthVerificationException ex = new ExternalAuthVerificationException("Service unavailable",
				ExternalAuthVerificationException.ErrorType.NETWORK_ERROR, null);

		ResponseEntity<Object> responseEntity = globalExceptionHandler.handleGoogleTokenVerification(ex);
		ErrorResponseDto body = (ErrorResponseDto) responseEntity.getBody();

		assertAll(() -> assertEquals(HttpStatus.SERVICE_UNAVAILABLE, responseEntity.getStatusCode()),
				() -> assertEquals(503, body.status()), () -> assertEquals("Service unavailable", body.message()),
				() -> assertNull(body.details()));
	}

	@Test
	void handleAuthenticationFailed_Returns401() {
		AuthenticationFailedException ex = new AuthenticationFailedException("Expired OTP");

		ResponseEntity<Object> responseEntity = globalExceptionHandler.handleAuthenticationFailed(ex);
		ErrorResponseDto body = (ErrorResponseDto) responseEntity.getBody();

		assertAll(() -> assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode()),
				() -> assertEquals(401, body.status()), () -> assertEquals("Expired OTP", body.message()));
	}

	@Test
	void handleTooManyRequests_Returns429() {
		TooManyRequestsException ex = new TooManyRequestsException("Limit exceeded");

		ResponseEntity<Object> responseEntity = globalExceptionHandler.handleTooManyRequests(ex);
		ErrorResponseDto body = (ErrorResponseDto) responseEntity.getBody();

		assertAll(() -> assertEquals(HttpStatus.TOO_MANY_REQUESTS, responseEntity.getStatusCode()),
				() -> assertEquals(429, body.status()), () -> assertEquals("Too Many Requests", body.message()));
	}

	@Test
	void handleUserNotFound_Returns404() {
		UserNotFoundException ex = new UserNotFoundException("User not found");

		ResponseEntity<Object> responseEntity = globalExceptionHandler.handleEntityNotFound(ex);
		ErrorResponseDto body = (ErrorResponseDto) responseEntity.getBody();

		assertAll(() -> assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode()),
				() -> assertEquals(404, body.status()), () -> assertEquals("User not found", body.message()));
	}

	@Test
	void handleBadCredentials_Returns401() {
		BadCredentialsException ex = new BadCredentialsException("Bad credentials");

		ResponseEntity<Object> responseEntity = globalExceptionHandler.handleBadCredentials(ex);
		ErrorResponseDto body = (ErrorResponseDto) responseEntity.getBody();

		assertAll(() -> assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode()),
				() -> assertEquals(401, body.status()));
	}

	@Test
	void handleAllExceptions_Returns500() {
		Exception ex = new RuntimeException("Unexpected error");

		ResponseEntity<Object> responseEntity = globalExceptionHandler.handleAllExceptions(ex);
		ErrorResponseDto body = (ErrorResponseDto) responseEntity.getBody();

		assertAll(() -> assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode()),
				() -> assertEquals(500, body.status()),
				() -> assertEquals("Something went wrong. Please try again later.", body.message()));
	}
}
