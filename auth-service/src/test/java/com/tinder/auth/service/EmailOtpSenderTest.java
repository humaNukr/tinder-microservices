package com.tinder.auth.service;

import com.tinder.auth.dto.otp.DeliveryChannel;
import com.tinder.auth.properties.MailProperties;
import com.tinder.auth.service.impl.EmailOtpSender;
import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailOtpSenderTest {

	@Mock
	private JavaMailSender mailSender;

	@Mock
	private TemplateEngine templateEngine;

	@Mock
	private MailProperties mailProperties;

	@InjectMocks
	private EmailOtpSender emailOtpSender;

	@Test
	void sendOtp_Success_SendsEmail() throws Exception {
		String destination = "user@example.com";
		Integer otp = 123456;
		String expectedHtml = "<html>123456</html>";
		String senderEmail = "noreply@tinder.com";

		MimeMessage mimeMessage = new MimeMessage((Session) null);
		when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
		when(mailProperties.username()).thenReturn(senderEmail);

		ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
		when(templateEngine.process(eq("otp-mail"), contextCaptor.capture())).thenReturn(expectedHtml);

		emailOtpSender.sendOtp(destination, otp);

		verify(mailSender).send(mimeMessage);

		Context capturedContext = contextCaptor.getValue();
		assertAll(() -> assertEquals(otp, capturedContext.getVariable("OTP_CODE")),
				() -> assertEquals(destination, mimeMessage.getAllRecipients()[0].toString()),
				() -> assertEquals(senderEmail, mimeMessage.getFrom()[0].toString()),
				() -> assertEquals("Your confirmation code", mimeMessage.getSubject()));
	}

	@Test
	void sendOtp_MessagingException_HandlesGracefully() throws Exception {
		MimeMessage mockMessage = mock(MimeMessage.class);
		when(mailSender.createMimeMessage()).thenReturn(mockMessage);
		when(mailProperties.username()).thenReturn("noreply@tinder.com");
		when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html></html>");

		doThrow(new MessagingException("Simulated error")).when(mockMessage).setFrom(any(Address.class));

		emailOtpSender.sendOtp("user@example.com", 123456);

		verify(mailSender, never()).send(any(MimeMessage.class));
	}

	@Test
	void supports_EmailChannel_ReturnsTrue() {
		assertTrue(emailOtpSender.supports(DeliveryChannel.EMAIL));
	}

	@Test
	void supports_NullChannel_ReturnsFalse() {
		assertFalse(emailOtpSender.supports(null));
	}
}
