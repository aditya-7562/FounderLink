package com.founderlink.notification.service;

import com.founderlink.notification.client.StartupServiceClient;
import com.founderlink.notification.client.UserServiceClient;
import com.founderlink.notification.command.NotificationCommandService;
import com.founderlink.notification.dto.NotificationResponseDTO;
import com.founderlink.notification.dto.StartupDTO;
import com.founderlink.notification.dto.UserDTO;
import com.founderlink.notification.entity.Notification;
import com.founderlink.notification.query.NotificationQueryService;
import com.founderlink.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationCommandService commandService;

    @Mock
    private NotificationQueryService queryService;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private StartupServiceClient startupServiceClient;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
    }

    @Test
    @DisplayName("Delegated Calls - Create and Mark Read")
    void delegatedWrites() {
        when(commandService.createNotification(anyLong(), anyString(), anyString())).thenReturn(NotificationResponseDTO.builder().build());
        notificationService.createNotification(100L, "T", "M");
        verify(commandService).createNotification(100L, "T", "M");

        notificationService.markAsRead(1L);
        verify(commandService).markAsRead(1L);

        notificationService.markAsReadForUser(1L, 100L);
        verify(commandService).markAsReadForUser(1L, 100L);
    }

    @Test
    @DisplayName("Delegated Calls - Querying")
    void delegatedReads() {
        notificationService.getNotificationsByUser(100L);
        verify(queryService).getNotificationsByUser(100L);

        Pageable p = PageRequest.of(0, 10);
        notificationService.getNotificationsByUser(100L, p);
        verify(queryService).getNotificationsByUser(100L, p);

        notificationService.getUnreadNotifications(100L);
        verify(queryService).getUnreadNotifications(100L);

        notificationService.getUnreadNotifications(100L, p);
        verify(queryService).getUnreadNotifications(100L, p);
    }

    @Test
    @DisplayName("notifyAllUsers - branch for createNotification failure")
    void notifyAllUsers_HandlesInnerFailure() {
        UserDTO user1 = new UserDTO(1L, "A", "a@t.com", "I", null, null, null, null);
        UserDTO user2 = new UserDTO(2L, "B", "b@t.com", "I", null, null, null, null);
        when(userServiceClient.getAllUsers()).thenReturn(List.of(user1, user2));
        
        doThrow(new RuntimeException("Err")).when(commandService).createNotification(eq(1L), anyString(), anyString());
        when(commandService.createNotification(eq(2L), anyString(), anyString())).thenReturn(NotificationResponseDTO.builder().build());

        notificationService.notifyAllUsers("TYPE", "MSG");

        verify(commandService, times(2)).createNotification(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("notifyAllUsers - retry/circuit breaker fallback path")
    void notifyAllUsers_Fallback() {
        notificationService.notifyAllUsersFallback("T", "M", new RuntimeException("E"));
    }

    @Test
    @DisplayName("sendStartupCreatedEmailToAllInvestors - branch coverage")
    void sendStartupCreated_BranchCoverage() {
        UserDTO i1 = new UserDTO(1L, "A", "a@t.com", "INVESTOR", null, null, null, null);
        UserDTO i2 = new UserDTO(2L, "B", null, "INVESTOR", null, null, null, null); // No email
        when(userServiceClient.getUsersByRole("INVESTOR")).thenReturn(List.of(i1, i2));
        doThrow(new RuntimeException("Err")).when(commandService).createNotification(eq(1L), anyString(), anyString());
        when(commandService.createNotification(eq(2L), anyString(), anyString())).thenReturn(NotificationResponseDTO.builder().build());

        notificationService.sendStartupCreatedEmailToAllInvestors(1L, "N", "I", 100.0);

        verify(emailService).sendBulkEmail(any(), any(), any());
    }

    @Test
    @DisplayName("sendInvestmentInterestEmailToFounder - missing info branches")
    void sendInterest_MissingInfoBranches() {
        // 1. startup null
        when(startupServiceClient.getStartupById(1L)).thenReturn(null);
        notificationService.sendInvestmentInterestEmailToFounder(1L, 200L, "B");
        verify(emailService, never()).sendEmail(any(), any(), any());

        // 2. founderId null
        StartupDTO s = new StartupDTO(1L, "N", null, null, null, null, null, null);
        when(startupServiceClient.getStartupById(1L)).thenReturn(s);
        notificationService.sendInvestmentInterestEmailToFounder(1L, 200L, "B");
        verify(emailService, never()).sendEmail(any(), any(), any());

        // 3. founder user null
        s.setFounderId(100L);
        when(userServiceClient.getUserById(100L)).thenReturn(null);
        notificationService.sendInvestmentInterestEmailToFounder(1L, 200L, "B");
        verify(emailService, never()).sendEmail(any(), any(), any());

        // 4. founder email null
        UserDTO f = new UserDTO(100L, "A", null, "F", null, null, null, null);
        when(userServiceClient.getUserById(100L)).thenReturn(f);
        notificationService.sendInvestmentInterestEmailToFounder(1L, 200L, "B");
        verify(emailService, never()).sendEmail(any(), any(), any());

        // Success cases
        f.setEmail("f@t.com");
        UserDTO inv = new UserDTO(200L, "Alice", "alice@t.com", "I", null, null, null, null);
        when(commandService.createNotification(anyLong(), anyString(), anyString())).thenReturn(NotificationResponseDTO.builder().build());

        // 5. resolve from provided investorName
        when(userServiceClient.getUserById(100L)).thenReturn(f);
        notificationService.sendInvestmentInterestEmailToFounder(1L, 200L, "Bob");
        verify(emailService).sendEmail(any(), any(), contains("Bob"));

        // 6. resolve from investor user name
        reset(emailService);
        when(userServiceClient.getUserById(100L)).thenReturn(f);
        when(userServiceClient.getUserById(200L)).thenReturn(inv);
        notificationService.sendInvestmentInterestEmailToFounder(1L, 200L, null);
        verify(emailService).sendEmail(any(), any(), contains("Alice"));

        // 7. resolve to "An investor" when both null
        reset(emailService);
        when(userServiceClient.getUserById(100L)).thenReturn(f);
        when(userServiceClient.getUserById(200L)).thenReturn(null);
        notificationService.sendInvestmentInterestEmailToFounder(1L, 200L, "");
        verify(emailService).sendEmail(any(), any(), contains("An investor"));
    }

    @Test
    @DisplayName("sendInvestmentCreatedEmailToFounder - edge cases")
    void sendInvestmentCreated_EdgeCases() {
        UserDTO f = new UserDTO(100L, "F", "f@t.com", "F", null, null, null, null);
        UserDTO inv = new UserDTO(200L, "Alice", null, "I", null, null, null, null); // investor found but no email? Wait, logic only uses name
        when(userServiceClient.getUserById(100L)).thenReturn(f);
        when(userServiceClient.getUserById(200L)).thenReturn(null); // Unknown investor
        when(commandService.createNotification(anyLong(), anyString(), anyString())).thenReturn(NotificationResponseDTO.builder().build());

        notificationService.sendInvestmentCreatedEmailToFounder(1L, 100L, 200L, 10.0);
        verify(emailService).sendEmail(any(), any(), contains("Unknown Investor"));

        // founder null
        when(userServiceClient.getUserById(100L)).thenReturn(null);
        notificationService.sendInvestmentCreatedEmailToFounder(1L, 100L, 200L, 10.0);
        
        // caught exception
        when(userServiceClient.getUserById(100L)).thenThrow(new RuntimeException("Err"));
        notificationService.sendInvestmentCreatedEmailToFounder(1L, 100L, 200L, 10.0);
    }

    @Test
    @DisplayName("Team Notification edge cases")
    void teamNotification_EdgeCases() {
        UserDTO f = new UserDTO(100L, null, "f@t.com", "F", null, null, null, null);
        when(userServiceClient.getUserById(100L)).thenReturn(f);
        when(userServiceClient.getUserById(200L)).thenReturn(null); // member null
        when(commandService.createNotification(anyLong(), anyString(), anyString())).thenReturn(NotificationResponseDTO.builder().build());

        // Accepted - name defaults
        notificationService.sendTeamMemberAcceptedNotification(1L, 100L, 200L, "CTO");
        verify(emailService).sendTeamMemberAcceptedEmail(any(), eq("Founder"), eq("A team member"), any(), any());

        // Rejected - name defaults
        reset(emailService);
        notificationService.sendTeamMemberRejectedNotification(1L, 100L, 200L, "CFO");
        verify(emailService).sendTeamMemberRejectedEmail(any(), eq("Founder"), eq("A team member"), any(), any());
    }

    @Test
    @DisplayName("Payment edge cases")
    void payment_EdgeCases() {
        UserDTO inv = new UserDTO(200L, null, "i@t.com", "I", null, null, null, null);
        UserDTO f = new UserDTO(100L, null, "f@t.com", "F", null, null, null, null);
        when(commandService.createNotification(anyLong(), anyString(), anyString())).thenReturn(NotificationResponseDTO.builder().build());

        // 1. Both found with email
        when(userServiceClient.getUserById(200L)).thenReturn(inv);
        when(userServiceClient.getUserById(100L)).thenReturn(f);
        notificationService.sendPaymentCompletedNotification(1L, 200L, 100L);
        verify(commandService, times(2)).createNotification(anyLong(), eq("PAYMENT_COMPLETED"), anyString());

        // 2. Investor found but NO email
        reset(commandService);
        inv.setEmail(null);
        notificationService.sendPaymentCompletedNotification(1L, 200L, 100L);
        verify(commandService, times(1)).createNotification(eq(100L), anyString(), anyString()); // Only founder

        // 3. Founder found but NO email
        reset(commandService);
        inv.setEmail("i@t.com");
        f.setEmail(null);
        notificationService.sendPaymentCompletedNotification(1L, 200L, 100L);
        verify(commandService, times(1)).createNotification(eq(200L), anyString(), anyString()); // Only investor

        // 4. Both found but NO email
        reset(commandService);
        inv.setEmail(null);
        f.setEmail(null);
        notificationService.sendPaymentCompletedNotification(1L, 200L, 100L);
        verify(commandService, never()).createNotification(anyLong(), anyString(), anyString());

        // Payment failed - name default
        reset(commandService);
        inv.setEmail("i@t.com");
        when(userServiceClient.getUserById(200L)).thenReturn(inv);
        notificationService.sendPaymentFailedNotification(1L, 200L, "Reason");
        verify(emailService).sendPaymentFailedEmail(any(), eq("Investor"), any(), any());

        // Invest approved/rejected - name defaults
        notificationService.sendInvestmentApprovedNotification(1L, 200L, 10L, "$10");
        verify(emailService).sendInvestmentApprovedEmail(any(), eq("Investor"), any(), any());

        notificationService.sendInvestmentRejectedNotification(1L, 200L, 10L, "$10", "R");
        verify(emailService).sendInvestmentRejectedEmail(any(), eq("Investor"), any(), any(), any());
    }

    @Test
    @DisplayName("Notification Service Exception catching")
    void catchBlocks() {
        // notifyAllUsers (RETHROWS)
        reset(userServiceClient);
        when(userServiceClient.getAllUsers()).thenThrow(new RuntimeException("E"));
        assertThatThrownBy(() -> notificationService.notifyAllUsers("T", "M")).isInstanceOf(RuntimeException.class);

        // others (CAUGHT)
        reset(userServiceClient);
        reset(startupServiceClient);
        when(userServiceClient.getUsersByRole("INVESTOR")).thenThrow(new RuntimeException("E"));
        notificationService.sendStartupCreatedEmailToAllInvestors(1L, "N", "I", 10.0);

        when(startupServiceClient.getStartupById(anyLong())).thenThrow(new RuntimeException("E"));
        notificationService.sendInvestmentInterestEmailToFounder(1L, 2L, "N");

        when(userServiceClient.getUserById(anyLong())).thenThrow(new RuntimeException("E"));
        notificationService.sendTeamInviteEmail(1L, 2L, "R");
        notificationService.sendTeamMemberAcceptedNotification(1L, 2L, 3L, "R");
        notificationService.sendTeamMemberRejectedNotification(1L, 2L, 3L, "R");
        notificationService.sendPaymentCompletedNotification(1L, 2L, 3L);
        notificationService.sendPaymentFailedNotification(1L, 2L, "R");
        notificationService.sendInvestmentApprovedNotification(1L, 2L, 3L, "A");
        notificationService.sendInvestmentRejectedNotification(1L, 2L, 3L, "A", "R");
    }
}
