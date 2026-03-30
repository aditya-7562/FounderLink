package com.founderlink.notification.service;

import org.junit.jupiter.api.BeforeEach;
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

    @BeforeEach
    void setUp() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));
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
        assertThat(message.getText()).contains("John");
        assertThat(message.getText()).contains("Bob");
        assertThat(message.getText()).contains("CFO");
        assertThat(message.getText()).contains("102");
    }

    @Test
    @DisplayName("sendPaymentCompletedEmail - sends email with correct content")
    void sendPaymentCompletedEmail_Success() {
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailService.sendPaymentCompletedEmail(
                "investor@test.com", "Alice", 1L, "$50,000"
        );

        verify(mailSender, times(1)).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();

        assertThat(message.getTo()).containsExactly("investor@test.com");
        assertThat(message.getSubject()).contains("Payment Successful");
        assertThat(message.getText()).contains("Alice");
        assertThat(message.getText()).contains("$50,000");
    }

    @Test
    @DisplayName("sendPaymentFailedEmail - sends email with correct content")
    void sendPaymentFailedEmail_Success() {
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailService.sendPaymentFailedEmail(
                "investor@test.com", "Bob", 2L, "Insufficient funds"
        );

        verify(mailSender, times(1)).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();

        assertThat(message.getTo()).containsExactly("investor@test.com");
        assertThat(message.getSubject()).contains("Payment Failed");
        assertThat(message.getText()).contains("Bob");
        assertThat(message.getText()).contains("Insufficient funds");
    }

    @Test
    @DisplayName("sendInvestmentApprovedEmail - sends email with correct content")
    void sendInvestmentApprovedEmail_Success() {
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailService.sendInvestmentApprovedEmail(
                "investor@test.com", "Charlie", 101L, "$100,000"
        );

        verify(mailSender, times(1)).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();

        assertThat(message.getTo()).containsExactly("investor@test.com");
        assertThat(message.getSubject()).contains("Investment Was Approved");
        assertThat(message.getText()).contains("Charlie");
        assertThat(message.getText()).contains("$100,000");
        assertThat(message.getText()).contains("101");
    }

    @Test
    @DisplayName("sendInvestmentRejectedEmail - sends email with correct content")
    void sendInvestmentRejectedEmail_Success() {
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailService.sendInvestmentRejectedEmail(
                "investor@test.com", "David", 102L, "$75,000", "Not aligned with goals"
        );

        verify(mailSender, times(1)).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();

        assertThat(message.getTo()).containsExactly("investor@test.com");
        assertThat(message.getSubject()).contains("Investment Was Rejected");
        assertThat(message.getText()).contains("David");
        assertThat(message.getText()).contains("$75,000");
        assertThat(message.getText()).contains("Not aligned with goals");
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
