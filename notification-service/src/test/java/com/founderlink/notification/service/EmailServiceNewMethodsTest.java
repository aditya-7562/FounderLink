package com.founderlink.notification.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceNewMethodsTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    @DisplayName("sendBulkEmail - sends multiple emails")
    void sendBulkEmail_Success() {
        String[] recipients = {"a@t.com", "b@t.com"};
        emailService.sendBulkEmail(recipients, "S", "B");
        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("sendPasswordResetPinEmail - correct content")
    void sendPasswordResetPinEmail_Success() {
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        emailService.sendPasswordResetPinEmail("a@t.com", "User", "123456");
        verify(mailSender).send(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getText()).contains("123456");
    }

    @Test
    @DisplayName("sendWelcomeEmail - correct content")
    void sendWelcomeEmail_Success() {
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        emailService.sendWelcomeEmail("a@t.com", "User", "INVESTOR");
        verify(mailSender).send(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getText()).contains("INVESTOR");
    }

    @Test
    @DisplayName("sendTeamMemberAcceptedEmail - sends email with correct content")
    void sendTeamMemberAcceptedEmail_Success() {
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailService.sendTeamMemberAcceptedEmail(
                "founder@test.com", "John", "Alice", "CTO", 101L
        );

        verify(mailSender, times(1)).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();

        assertThat(message.getTo()).containsExactly("founder@test.com");
        assertThat(message.getSubject()).contains("Team Member Accepted");
        assertThat(message.getText()).contains("John");
        assertThat(message.getText()).contains("Alice");
        assertThat(message.getText()).contains("CTO");
        assertThat(message.getText()).contains("101");
    }

    @Test
    @DisplayName("sendTeamMemberRejectedEmail - sends email with correct content")
    void sendTeamMemberRejectedEmail_Success() {
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailService.sendTeamMemberRejectedEmail(
                "founder@test.com", "John", "Bob", "CFO", 102L
        );

        verify(mailSender, times(1)).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();

        assertThat(message.getTo()).containsExactly("founder@test.com");
        assertThat(message.getSubject()).contains("Team Member Rejected");
    }

    @Test
    @DisplayName("sendPaymentCompletedEmail - sends email with correct content")
    void sendPaymentCompletedEmail_Success() {
        emailService.sendPaymentCompletedEmail("investor@test.com", "Alice", 1L, "$50,000");
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("sendPaymentFailedEmail - sends email with correct content")
    void sendPaymentFailedEmail_Success() {
        emailService.sendPaymentFailedEmail("investor@test.com", "Bob", 2L, "Insufficient funds");
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("sendInvestmentApprovedEmail - sends email with correct content")
    void sendInvestmentApprovedEmail_Success() {
        emailService.sendInvestmentApprovedEmail("investor@test.com", "Charlie", 101L, "$100,000");
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("sendInvestmentRejectedEmail - sends email with correct content")
    void sendInvestmentRejectedEmail_Success() {
        emailService.sendInvestmentRejectedEmail("investor@test.com", "David", 102L, "$75,000", "Reason");
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("sendInvestmentRejectedEmail - handles null reason")
    void sendInvestmentRejectedEmail_NullReason() {
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailService.sendInvestmentRejectedEmail(
                "investor@test.com", "Eve", 103L, "$50,000", null
        );

        verify(mailSender, times(1)).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();

        assertThat(message.getText()).contains("Not specified");
    }

    @Test
    @DisplayName("sendEmail - handles exception gracefully")
    void sendEmail_HandlesException() {
        doThrow(new RuntimeException("SMTP server unavailable"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendEmail("test@test.com", "Subject", "Body");

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}
