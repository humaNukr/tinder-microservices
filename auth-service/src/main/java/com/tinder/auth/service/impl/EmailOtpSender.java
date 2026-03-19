package com.tinder.auth.service.impl;

import com.tinder.auth.properties.MailProperties;
import com.tinder.auth.service.interfaces.OtpSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;


@Service
@RequiredArgsConstructor
@Slf4j
public class EmailOtpSender implements OtpSender {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final MailProperties mailProperties;
    private static final String TEMPLATE_NAME = "otp-mail";
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

    @Async
    @Override
    public void sendOtp(String destination, Integer otp) {
        try {
            Context thymeleafContext = new Context();
            thymeleafContext.setVariable("OTP_CODE", otp);

            String htmlBody = templateEngine.process(TEMPLATE_NAME , thymeleafContext);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailProperties.getUsername());
            helper.setTo(destination);
            helper.setSubject("Your confirmation code");
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("Email sent successfully to {}", destination);

        } catch (MessagingException e) {
            log.error("Failed to send email to {}", destination, e);
        }
    }

    @Override
    public boolean supports(String identifier) {
        return identifier.matches(EMAIL_REGEX);
    }
}
