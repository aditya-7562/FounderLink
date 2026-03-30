package com.founderlink.notification.service;

import com.founderlink.notification.client.UserServiceClient;
import com.founderlink.notification.command.NotificationCommandService;
import com.founderlink.notification.dto.NotificationResponseDTO;
import com.founderlink.notification.dto.UserDTO;
import com.founderlink.notification.query.NotificationQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceNewMethodsTest {

    @Mock
    private NotificationCommandService commandService;

    @Mock
    private NotificationQueryService queryService;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private com.founderlink.notification.client.StartupServiceClient startupServiceClient;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("sendTeamMemberAcceptedNotification - sends notification and email")
    void sendTeamMemberAcceptedNotification_Success() {
        UserDTO founder = new UserDTO(5L, "John", "john@test.com", "FOUNDER", null, null, null, null);
        UserDTO acceptedUser = new UserDTO(300L, "Alice", "alice@test.com", "COFOUNDER", null, null, null, null);

        when(userServiceClient.getUserById(5L)).thenReturn(founder);
        when(userServiceClient.getUserById(300L)).thenReturn(acceptedUser);
        when(commandService.createNotification(anyLong(), anyString(), anyString()))
                .thenReturn(NotificationResponseDTO.builder().build());

        notificationService.sendTeamMemberAcceptedNotification(101L, 5L, 300L, "CTO");

        verify(commandService, times(1)).createNotification(eq(5L), eq("TEAM_MEMBER_ACCEPTED"), anyString());
        verify(emailService, times(1)).sendTeamMemberAcceptedEmail(
                eq("john@test.com"), eq("John"), eq("Alice"), eq("CTO"), eq(101L)
        );
    }

    @Test
    @DisplayName("sendTeamMemberAcceptedNotification - skips when founder has no email")
    void sendTeamMemberAcceptedNotification_NoFounderEmail() {
        UserDTO founder = new UserDTO(5L, "John", null, "FOUNDER", null, null, null, null);

        when(userServiceClient.getUserById(5L)).thenReturn(founder);

        notificationService.sendTeamMemberAcceptedNotification(101L, 5L, 300L, "CTO");

        verify(commandService, never()).createNotification(anyLong(), anyString(), anyString());
        verify(emailService, never()).sendTeamMemberAcceptedEmail(anyString(), anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("sendTeamMemberRejectedNotification - sends notification and email")
    void sendTeamMemberRejectedNotification_Success() {
        UserDTO founder = new UserDTO(5L, "John", "john@test.com", "FOUNDER", null, null, null, null);
        UserDTO rejectedUser = new UserDTO(300L, "Bob", "bob@test.com", "COFOUNDER", null, null, null, null);

        when(userServiceClient.getUserById(5L)).thenReturn(founder);
        when(userServiceClient.getUserById(300L)).thenReturn(rejectedUser);
        when(commandService.createNotification(anyLong(), anyString(), anyString()))
                .thenReturn(NotificationResponseDTO.builder().build());

        notificationService.sendTeamMemberRejectedNotification(101L, 5L, 300L, "CFO");

        verify(commandService, times(1)).createNotification(eq(5L), eq("TEAM_MEMBER_REJECTED"), anyString());
        verify(emailService, times(1)).sendTeamMemberRejectedEmail(
                eq("john@test.com"), eq("John"), eq("Bob"), eq("CFO"), eq(101L)
        );
    }

    @Test
    @DisplayName("sendPaymentCompletedNotification - sends notifications to investor and founder")
    void sendPaymentCompletedNotification_Success() {
        UserDTO investor = new UserDTO(200L, "Alice", "alice@test.com", "INVESTOR", null, null, null, null);
        UserDTO founder = new UserDTO(100L, "John", "john@test.com", "FOUNDER", null, null, null, null);

        when(userServiceClient.getUserById(200L)).thenReturn(investor);
        when(userServiceClient.getUserById(100L)).thenReturn(founder);
        when(commandService.createNotification(anyLong(), anyString(), anyString()))
                .thenReturn(NotificationResponseDTO.builder().build());

        notificationService.sendPaymentCompletedNotification(1L, 200L, 100L);

        verify(commandService, times(2)).createNotification(anyLong(), eq("PAYMENT_COMPLETED"), anyString());
        verify(emailService, times(1)).sendPaymentCompletedEmail(
                eq("alice@test.com"), eq("Alice"), eq(1L), anyString()
        );
    }

    @Test
    @DisplayName("sendPaymentFailedNotification - sends notification to investor")
    void sendPaymentFailedNotification_Success() {
        UserDTO investor = new UserDTO(200L, "Bob", "bob@test.com", "INVESTOR", null, null, null, null);

        when(userServiceClient.getUserById(200L)).thenReturn(investor);
        when(commandService.createNotification(anyLong(), anyString(), anyString()))
                .thenReturn(NotificationResponseDTO.builder().build());

        notificationService.sendPaymentFailedNotification(2L, 200L, "Insufficient funds");

        verify(commandService, times(1)).createNotification(eq(200L), eq("PAYMENT_FAILED"), anyString());
        verify(emailService, times(1)).sendPaymentFailedEmail(
                eq("bob@test.com"), eq("Bob"), eq(2L), eq("Insufficient funds")
        );
    }

    @Test
    @DisplayName("sendInvestmentApprovedNotification - sends notification to investor")
    void sendInvestmentApprovedNotification_Success() {
        UserDTO investor = new UserDTO(200L, "Charlie", "charlie@test.com", "INVESTOR", null, null, null, null);

        when(userServiceClient.getUserById(200L)).thenReturn(investor);
        when(commandService.createNotification(anyLong(), anyString(), anyString()))
                .thenReturn(NotificationResponseDTO.builder().build());

        notificationService.sendInvestmentApprovedNotification(1L, 200L, 101L, "$100,000");

        verify(commandService, times(1)).createNotification(eq(200L), eq("INVESTMENT_APPROVED"), anyString());
        verify(emailService, times(1)).sendInvestmentApprovedEmail(
                eq("charlie@test.com"), eq("Charlie"), eq(101L), eq("$100,000")
        );
    }

    @Test
    @DisplayName("sendInvestmentRejectedNotification - sends notification to investor")
    void sendInvestmentRejectedNotification_Success() {
        UserDTO investor = new UserDTO(200L, "David", "david@test.com", "INVESTOR", null, null, null, null);

        when(userServiceClient.getUserById(200L)).thenReturn(investor);
        when(commandService.createNotification(anyLong(), anyString(), anyString()))
                .thenReturn(NotificationResponseDTO.builder().build());

        notificationService.sendInvestmentRejectedNotification(2L, 200L, 102L, "$75,000", "Not aligned");

        verify(commandService, times(1)).createNotification(eq(200L), eq("INVESTMENT_REJECTED"), anyString());
        verify(emailService, times(1)).sendInvestmentRejectedEmail(
                eq("david@test.com"), eq("David"), eq(102L), eq("$75,000"), eq("Not aligned")
        );
    }

    @Test
    @DisplayName("sendInvestmentApprovedNotification - skips when investor not found")
    void sendInvestmentApprovedNotification_InvestorNotFound() {
        when(userServiceClient.getUserById(200L)).thenReturn(null);

        notificationService.sendInvestmentApprovedNotification(1L, 200L, 101L, "$100,000");

        verify(commandService, never()).createNotification(anyLong(), anyString(), anyString());
        verify(emailService, never()).sendInvestmentApprovedEmail(anyString(), anyString(), anyLong(), anyString());
    }

    @Test
    @DisplayName("sendPaymentFailedNotification - skips when investor has no email")
    void sendPaymentFailedNotification_NoInvestorEmail() {
        UserDTO investor = new UserDTO(200L, "Eve", null, "INVESTOR", null, null, null, null);

        when(userServiceClient.getUserById(200L)).thenReturn(investor);

        notificationService.sendPaymentFailedNotification(3L, 200L, "Card declined");

        verify(commandService, never()).createNotification(anyLong(), anyString(), anyString());
        verify(emailService, never()).sendPaymentFailedEmail(anyString(), anyString(), anyLong(), anyString());
    }
}
