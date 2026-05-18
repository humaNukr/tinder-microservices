package com.tinder.auth.service;

import com.tinder.auth.dto.otp.DeliveryChannel;
import com.tinder.auth.service.impl.OtpServiceImpl;
import com.tinder.auth.service.interfaces.OtpSender;
import com.tinder.auth.service.interfaces.OtpStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.SecureRandom;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OtpServiceImplTest {

	private final String destination = "test@example.com";
	@Mock
	private OtpStorage otpStorage;
	@Mock
	private SecureRandom secureRandom;
	@Mock
	private OtpSender emailSender;
	@Mock
	private OtpSender smsSender;
	private OtpServiceImpl otpService;

	@BeforeEach
	void setUp() {
		otpService = new OtpServiceImpl(List.of(emailSender, smsSender), otpStorage, secureRandom);
	}

	@Nested
	@DisplayName("generateAndSendOtp() Tests")
	class GenerateAndSendOtpTests {

		@Test
        void generateAndSendOtp_ValidRequest_GeneratesSavesAndSends() {
            when(secureRandom.nextInt(900000)).thenReturn(234567);
            when(emailSender.supports(DeliveryChannel.EMAIL)).thenReturn(true);

            otpService.generateAndSendOtp(destination, DeliveryChannel.EMAIL);

            assertAll(
                    () -> verify(otpStorage).checkAndIncrementRateLimit(destination),
                    () -> verify(otpStorage).saveOtp(destination, "334567"),
                    () -> verify(emailSender).sendOtp(destination, 334567),
                    () -> verify(smsSender, never()).sendOtp(anyString(), any())
            );
        }

		@Test
        void generateAndSendOtp_UnsupportedChannel_ThrowsException() {
            when(secureRandom.nextInt(900000)).thenReturn(111111);
            when(emailSender.supports(any())).thenReturn(false);
            when(smsSender.supports(any())).thenReturn(false);

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> otpService.generateAndSendOtp(destination, DeliveryChannel.EMAIL));

            assertAll(
                    () -> assertEquals("No sender found for channel: EMAIL", exception.getMessage()),
                    () -> verify(otpStorage).checkAndIncrementRateLimit(destination),
                    () -> verify(otpStorage).saveOtp(destination, "211111"),
                    () -> verify(emailSender, never()).sendOtp(anyString(), any()),
                    () -> verify(smsSender, never()).sendOtp(anyString(), any())
            );
        }
	}

	@Nested
	@DisplayName("validateOtp() Tests")
	class ValidateOtpTests {

		@Test
		void validateOtp_ValidCode_ReturnsTrueAndDeletesOtp() {
			String validCode = "123456";
			when(otpStorage.getOtp(destination)).thenReturn(validCode);

			boolean result = otpService.validateOtp(destination, validCode);

			assertAll(() -> assertTrue(result), () -> verify(otpStorage).deleteOtp(destination));
		}

		@Test
        void validateOtp_InvalidCode_ReturnsFalseAndKeepsOtp() {
            when(otpStorage.getOtp(destination)).thenReturn("123456");

            boolean result = otpService.validateOtp(destination, "654321");

            assertAll(
                    () -> assertFalse(result),
                    () -> verify(otpStorage, never()).deleteOtp(anyString())
            );
        }

		@Test
        void validateOtp_NullCodeFromStorage_ReturnsFalse() {
            when(otpStorage.getOtp(destination)).thenReturn(null);

            boolean result = otpService.validateOtp(destination, "123456");

            assertAll(
                    () -> assertFalse(result),
                    () -> verify(otpStorage, never()).deleteOtp(anyString())
            );
        }
	}
}
