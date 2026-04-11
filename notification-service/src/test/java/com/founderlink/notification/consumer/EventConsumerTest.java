package com.founderlink.notification.consumer;

import com.founderlink.notification.dto.*;
import com.founderlink.notification.service.EmailService;
import com.founderlink.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventConsumerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EventConsumer eventConsumer;

    private NotificationResponseDTO mockResponse;

    @BeforeEach
    void setUp() {
        mockResponse = NotificationResponseDTO.builder()
                .id(1L).userId(100L).type("TEST")
                .message("Test").read(false)
                .createdAt(LocalDateTime.now()).build();
    }

    // --- handleStartupCreated tests ---

    @Test
    @DisplayName("handleStartupCreated - processes event correctly")
    void handleStartupCreated_ProcessesEvent() {
        Map<String, Object> event = new HashMap<>();
        event.put("startupId", 1);
        event.put("startupName", "TechStartup");
        event.put("industry", "Tech");
        event.put("fundingGoal", 500000.0);

        eventConsumer.handleStartupCreated(event);

        verify(notificationService).sendStartupCreatedEmailToAllInvestors(
                eq(1L), eq("TechStartup"), eq("Tech"), eq(500000.0));
    }

    @Test
    @DisplayName("handleStartupCreated - uses default name when startupName is null")
    void handleStartupCreated_UsesDefaultNameWhenStartupNameNull() {
        Map<String, Object> event = new HashMap<>();
        event.put("startupId", 2);
        event.put("startupName", null);
        event.put("industry", "Health");
        event.put("fundingGoal", 100000.0);

        eventConsumer.handleStartupCreated(event);

        verify(notificationService).sendStartupCreatedEmailToAllInvestors(
                eq(2L), eq("New Startup"), eq("Health"), eq(100000.0));
    }

    @Test
    @DisplayName("handleStartupCreated - handles parsing error")
    void handleStartupCreated_HandlesError() {
        Map<String, Object> event = new HashMap<>();
        event.put("startupId", "invalid");
        eventConsumer.handleStartupCreated(event);
    }

    @Test
    @DisplayName("handleStartupCreatedFallback - logs without throwing")
    void handleStartupCreatedFallback_Logs() {
        eventConsumer.handleStartupCreatedFallback(new HashMap<>(), new RuntimeException("fail"));
    }

    // --- handleInvestmentCreated tests ---

    @Test
    @DisplayName("handleInvestmentCreated - processes event correctly")
    void handleInvestmentCreated_ProcessesEvent() {
        Map<String, Object> event = new HashMap<>();
        event.put("investorId", 200);
        event.put("startupId", 1);
        event.put("amount", 50000.0);
        event.put("founderId", 100);

        eventConsumer.handleInvestmentCreated(event);

        verify(notificationService).sendInvestmentCreatedEmailToFounder(
                eq(1L), eq(100L), eq(200L), eq(50000.0));
    }

    @Test
    @DisplayName("handleInvestmentCreated - handles parsing error")
    void handleInvestmentCreated_HandlesError() {
        Map<String, Object> event = new HashMap<>();
        event.put("investorId", "invalid");
        eventConsumer.handleInvestmentCreated(event);
    }

    @Test
    @DisplayName("handleInvestmentCreatedFallback - logs without throwing")
    void handleInvestmentCreatedFallback_Logs() {
        eventConsumer.handleInvestmentCreatedFallback(new HashMap<>(), new RuntimeException("fail"));
    }

    // --- handleTeamInvite tests ---

    @Test
    @DisplayName("handleTeamInvite - processes event correctly")
    void handleTeamInvite_ProcessesEvent() {
        Map<String, Object> event = new HashMap<>();
        event.put("invitedUserId", 300);
        event.put("startupId", 1);
        event.put("role", "CTO");

        when(notificationService.createNotification(eq(300L), eq("TEAM_INVITE_SENT"), anyString()))
                .thenReturn(mockResponse);

        eventConsumer.handleTeamInvite(event);

        verify(notificationService).createNotification(eq(300L), eq("TEAM_INVITE_SENT"), anyString());
        verify(notificationService).sendTeamInviteEmail(1L, 300L, "CTO");
    }

    @Test
    @DisplayName("handleTeamInvite - handles parsing error")
    void handleTeamInvite_HandlesError() {
        Map<String, Object> event = new HashMap<>();
        event.put("invitedUserId", "invalid");
        eventConsumer.handleTeamInvite(event);
    }

    @Test
    @DisplayName("handleTeamInviteFallback - logs without throwing")
    void handleTeamInviteFallback_Logs() {
        eventConsumer.handleTeamInviteFallback(new HashMap<>(), new RuntimeException("fail"));
    }

    // --- handleMessageSent tests ---

    @Test
    @DisplayName("handleMessageSent - processes event correctly")
    void handleMessageSent_ProcessesEvent() {
        Map<String, Object> event = new HashMap<>();
        event.put("receiverId", 100);
        event.put("senderId", 200);
        event.put("senderName", "Alice");

        eventConsumer.handleMessageSent(event);

        verify(notificationService).createNotification(eq(100L), eq("MESSAGE_RECEIVED"), contains("Alice"));
    }

    @Test
    @DisplayName("handleMessageSent - handles parsing error")
    void handleMessageSent_HandlesError() {
        Map<String, Object> event = new HashMap<>();
        event.put("receiverId", "invalid");
        eventConsumer.handleMessageSent(event);
    }

    @Test
    @DisplayName("handleMessageSent - uses default name when senderName is null")
    void handleMessageSent_NullName() {
        Map<String, Object> event = new HashMap<>();
        event.put("receiverId", 100);
        event.put("senderId", 200);
        event.put("senderName", null);

        eventConsumer.handleMessageSent(event);

        verify(notificationService).createNotification(eq(100L), eq("MESSAGE_RECEIVED"), contains("Someone"));
    }

    @Test
    @DisplayName("handleMessageSentFallback - logs")
    void handleMessageSentFallback_Logs() {
        eventConsumer.handleMessageSentFallback(new HashMap<>(), new RuntimeException("fail"));
    }

    // --- handlePasswordResetEvent tests ---

    @Test
    @DisplayName("handlePasswordResetEvent - success")
    void handlePasswordReset_Success() {
        PasswordResetEmailEvent event = new PasswordResetEmailEvent("test@test.com", "1234", "User");
        eventConsumer.handlePasswordResetEvent(event);
        verify(emailService).sendPasswordResetPinEmail("test@test.com", "User", "1234");
    }

    @Test
    @DisplayName("handlePasswordResetEvent - catch branch")
    void handlePasswordReset_Catch() {
        doThrow(new RuntimeException("Err")).when(emailService).sendPasswordResetPinEmail(any(), any(), any());
        eventConsumer.handlePasswordResetEvent(new PasswordResetEmailEvent("e", "p", "u"));
    }

    @Test
    @DisplayName("handlePasswordResetFallback - logs")
    void handlePasswordResetFallback_Logs() {
        eventConsumer.handlePasswordResetFallback(new PasswordResetEmailEvent(), new RuntimeException("fail"));
    }

    // --- handleTeamMemberAccepted tests ---

    @Test
    @DisplayName("handleTeamMemberAccepted - success")
    void handleTeamMemberAccepted_Success() {
        TeamMemberAcceptedEvent event = new TeamMemberAcceptedEvent(10L, 1L, 100L, 200L, "CTO");
        eventConsumer.handleTeamMemberAccepted(event);
        verify(notificationService).sendTeamMemberAcceptedNotification(1L, 100L, 200L, "CTO");
    }

    @Test
    @DisplayName("handleTeamMemberAccepted - catch branch")
    void handleTeamMemberAccepted_Catch() {
        doThrow(new RuntimeException("Err")).when(notificationService).sendTeamMemberAcceptedNotification(anyLong(), anyLong(), anyLong(), anyString());
        eventConsumer.handleTeamMemberAccepted(new TeamMemberAcceptedEvent(1L, 2L, 3L, 4L, "R"));
    }

    // --- handleTeamMemberRejected tests ---

    @Test
    @DisplayName("handleTeamMemberRejected - success")
    void handleTeamMemberRejected_Success() {
        TeamMemberRejectedEvent event = new TeamMemberRejectedEvent(10L, 1L, 100L, 200L, "CFO");
        eventConsumer.handleTeamMemberRejected(event);
        verify(notificationService).sendTeamMemberRejectedNotification(1L, 100L, 200L, "CFO");
    }

    @Test
    @DisplayName("handleTeamMemberRejected - catch branch")
    void handleTeamMemberRejected_Catch() {
        doThrow(new RuntimeException("Err")).when(notificationService).sendTeamMemberRejectedNotification(anyLong(), anyLong(), anyLong(), anyString());
        eventConsumer.handleTeamMemberRejected(new TeamMemberRejectedEvent(1L, 2L, 3L, 4L, "R"));
    }

    // --- handlePaymentCompleted tests ---

    @Test
    @DisplayName("handlePaymentCompleted - success")
    void handlePaymentCompleted_Success() {
        PaymentCompletedEvent event = new PaymentCompletedEvent(1L, 2L, 200L, 100L, 10L, BigDecimal.valueOf(100.0));
        eventConsumer.handlePaymentCompleted(event);
        verify(notificationService).sendPaymentCompletedNotification(1L, 200L, 100L);
    }

    @Test
    @DisplayName("handlePaymentCompleted - catch branch")
    void handlePaymentCompleted_Catch() {
        doThrow(new RuntimeException("Err")).when(notificationService).sendPaymentCompletedNotification(anyLong(), anyLong(), anyLong());
        eventConsumer.handlePaymentCompleted(new PaymentCompletedEvent(1L, 2L, 3L, 4L, 5L, BigDecimal.ONE));
    }

    // --- handlePaymentFailed tests ---

    @Test
    @DisplayName("handlePaymentFailed - success")
    void handlePaymentFailed_Success() {
        PaymentFailedEvent event = new PaymentFailedEvent(1L, 2L, 200L, 100L, 10L, BigDecimal.valueOf(100.0), "Error");
        eventConsumer.handlePaymentFailed(event);
        verify(notificationService).sendPaymentFailedNotification(1L, 200L, "Error");
    }

    @Test
    @DisplayName("handlePaymentFailed - catch branch")
    void handlePaymentFailed_Catch() {
        doThrow(new RuntimeException("Err")).when(notificationService).sendPaymentFailedNotification(anyLong(), anyLong(), anyString());
        eventConsumer.handlePaymentFailed(new PaymentFailedEvent(1L, 2L, 3L, 4L, 5L, BigDecimal.ONE, "R"));
    }

    // --- handleInvestmentApproved tests ---

    @Test
    @DisplayName("handleInvestmentApproved - success")
    void handleInvestmentApproved_Success() {
        InvestmentApprovedEvent event = new InvestmentApprovedEvent(1L, 200L, 100L, 10L, BigDecimal.valueOf(50000.0));
        eventConsumer.handleInvestmentApproved(event);
        verify(notificationService).sendInvestmentApprovedNotification(eq(1L), eq(200L), eq(10L), contains("50000"));
    }

    @Test
    @DisplayName("handleInvestmentApproved - catch branch")
    void handleInvestmentApproved_Catch() {
        doThrow(new RuntimeException("Err")).when(notificationService).sendInvestmentApprovedNotification(anyLong(), anyLong(), anyLong(), anyString());
        eventConsumer.handleInvestmentApproved(new InvestmentApprovedEvent(1L, 2L, 3L, 4L, BigDecimal.ONE));
    }

    @Test
    @DisplayName("handleInvestmentApproved - handles null amount")
    void handleInvestmentApproved_NullAmount() {
        InvestmentApprovedEvent event = new InvestmentApprovedEvent(1L, 200L, 100L, 10L, null);
        eventConsumer.handleInvestmentApproved(event);
        verify(notificationService).sendInvestmentApprovedNotification(eq(1L), eq(200L), eq(10L), eq("N/A"));
    }

    // --- handleInvestmentRejected tests ---

    @Test
    @DisplayName("handleInvestmentRejected - success")
    void handleInvestmentRejected_Success() {
        InvestmentRejectedEvent event = new InvestmentRejectedEvent(1L, 200L, 100L, 10L, BigDecimal.valueOf(50000.0), "Reason");
        eventConsumer.handleInvestmentRejected(event);
        verify(notificationService).sendInvestmentRejectedNotification(eq(1L), eq(200L), eq(10L), contains("50000"), eq("Reason"));
    }

    @Test
    @DisplayName("handleInvestmentRejected - catch branch")
    void handleInvestmentRejected_Catch() {
        doThrow(new RuntimeException("Err")).when(notificationService).sendInvestmentRejectedNotification(anyLong(), anyLong(), anyLong(), anyString(), anyString());
        eventConsumer.handleInvestmentRejected(new InvestmentRejectedEvent(1L, 2L, 3L, 4L, BigDecimal.ONE, "R"));
    }

    // --- handleUserRegistered tests ---

    @Test
    @DisplayName("handleUserRegistered - success")
    void handleUserRegistered_Success() {
        UserRegisteredEvent event = new UserRegisteredEvent(1L, "test@test.com", "User", "INVESTOR");
        eventConsumer.handleUserRegistered(event);
        verify(emailService).sendWelcomeEmail("test@test.com", "User", "INVESTOR");
    }

    @Test
    @DisplayName("handleUserRegisteredFallback - logs")
    void handleUserRegisteredFallback_Logs() {
        eventConsumer.handleUserRegisteredFallback(new UserRegisteredEvent(), new RuntimeException("fail"));
    }

    @Test
    @DisplayName("Other Fallbacks")
    void otherFallbacks() {
        eventConsumer.handleTeamMemberRejectedFallback(new TeamMemberRejectedEvent(), new RuntimeException("f"));
        eventConsumer.handlePaymentCompletedFallback(new PaymentCompletedEvent(), new RuntimeException("f"));
        eventConsumer.handlePaymentFailedFallback(new PaymentFailedEvent(), new RuntimeException("f"));
        eventConsumer.handleInvestmentApprovedFallback(new InvestmentApprovedEvent(), new RuntimeException("f"));
        eventConsumer.handleInvestmentRejectedFallback(new InvestmentRejectedEvent(), new RuntimeException("f"));
        eventConsumer.handleMessageSentFallback(new HashMap<>(), new RuntimeException("f"));
    }

    @Test
    @DisplayName("Exception branches in handlers")
    void exceptionBranches() {
        doThrow(new RuntimeException("fail")).when(notificationService).sendStartupCreatedEmailToAllInvestors(any(), any(), any(), any());
        Map<String, Object> event = new HashMap<>();
        event.put("startupId", 1);
        event.put("startupName", "N");
        event.put("industry", "I");
        event.put("fundingGoal", 10.0);
        eventConsumer.handleStartupCreated(event);

        doThrow(new RuntimeException("fail")).when(emailService).sendWelcomeEmail(any(), any(), any());
        eventConsumer.handleUserRegistered(new UserRegisteredEvent(1L, "e", "n", "r"));
    }
}
