package com.tinder.auth.dto.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponseDto(LocalDateTime timestamp, int status, String error, String message,
		@JsonInclude(JsonInclude.Include.NON_EMPTY) Map<String, String> details) {
}
