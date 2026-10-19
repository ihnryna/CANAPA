
package org.spring.canapa.backend.notification;

import org.junit.jupiter.api.Test;
import org.spring.canapa.backend.notification.service.EmailService;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailServiceSmtpTest {

    @Test
    void shouldSendRealEmailThroughGmail() {
        String username = System.getenv("CANAPA_MAIL_USERNAME");
        String password = System.getenv("CANAPA_MAIL_PASSWORD");
        String enabledValue = System.getenv("CANAPA_MAIL_ENABLED");

        assertFalse(
                username == null || username.isBlank(),
                "Set CANAPA_MAIL_USERNAME in IntelliJ environment variables"
        );

        assertFalse(
                password == null || password.isBlank(),
                "Set CANAPA_MAIL_PASSWORD in IntelliJ environment variables"
        );

        assertTrue(
                Boolean.parseBoolean(enabledValue),
                "Set CANAPA_MAIL_ENABLED=true to send a real email"
        );

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);
        mailSender.setUsername(username);
        mailSender.setPassword(password);
        mailSender.setProtocol("smtp");

        Properties properties = mailSender.getJavaMailProperties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.starttls.required", "true");
        properties.put("mail.smtp.connectiontimeout", "10000");
        properties.put("mail.smtp.timeout", "10000");
        properties.put("mail.smtp.writetimeout", "10000");

        EmailService emailService = new EmailService(
                mailSender,
                username,
                true
        );

        assertDoesNotThrow(() -> emailService.sendTextEmail(
                username,
                "CANAPA - Gmail SMTP test",
                """
                Hello!

                This is a real test email from the CANAPA backend.

                If you received this message, Gmail SMTP is configured correctly.
                """
        ));
    }
}
