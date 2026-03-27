package com.founderlink.notification.service;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            message.setFrom("noreply@founderlink.com");

            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    public void sendBulkEmail(String[] recipients, String subject, String body) {
        for (String email : recipients) {
            sendEmail(email, subject, body);
        }
    }

    public void sendPasswordResetPinEmail(String to, String userName, String pin) {
        String subject = "Password Reset Request - FounderLink";
        String body = String.format(
                "Hello %s,\n\n" +
                "You have requested to reset your password.\n\n" +
                "Your 6-digit PIN is: %s\n\n" +
                "This PIN will expire in 5 minutes.\n\n" +
                "If you did not request this, please ignore this email.\n\n" +
                "Best regards,\n" +
                "FounderLink Team",
                userName, pin
        );
        sendEmail(to, subject, body);
    }
}
