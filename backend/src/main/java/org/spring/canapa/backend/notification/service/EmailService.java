
package org.spring.canapa.backend.notification.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final boolean enabled;

    public EmailService(
        JavaMailSender mailSender,
        @Value("${spring.mail.username:}") String fromAddress,
        @Value("${canapa.mail.enabled:false}") boolean enabled
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.enabled = enabled;
    }

    public void sendTextEmail(
        String recipient,
        String subject,
        String body
    ) {
        if (!enabled) {
            throw new IllegalStateException(
                "Email sending is disabled"
            );
        }

        if (fromAddress == null || fromAddress.isBlank()) {
            throw new IllegalStateException(
                "Email sender address is not configured"
            );
        }

        if (recipient == null || recipient.isBlank()) {
            throw new IllegalArgumentException(
                "Recipient email must not be blank"
            );
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
    }
}
