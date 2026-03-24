package com.founderlink.notification.service;

import com.founderlink.notification.client.StartupServiceClient;
import com.founderlink.notification.client.UserServiceClient;
import com.founderlink.notification.dto.NotificationResponseDTO;
import com.founderlink.notification.dto.StartupDTO;
import com.founderlink.notification.dto.UserDTO;
import com.founderlink.notification.entity.Notification;
import com.founderlink.notification.exception.NotificationNotFoundException;
import com.founderlink.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private StartupServiceClient startupServiceClient;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificationService notificationService;

    private Notification notification1;
    private Notification notification2;
    private Notification unreadNotification;

    @BeforeEach
    void setUp() {
        notification1 = new Notification();
        notification1.setId(1L);
        notification1.setUserId(100L);
        notification1.setType("STARTUP_CREATED");
        notification1.setMessage("New startup created in Tech industry");
        notification1.setRead(true);
        notification1.setCreatedAt(LocalDateTime.now().minusHours(2));

        notification2 = new Notification();
        notification2.setId(2L);
        notification2.setUserId(100L);
        notification2.setType("INVESTMENT_CREATED");
        notification2.setMessage("New investment of $50000");
        notification2.setRead(true);
        notification2.setCreatedAt(LocalDateTime.now().minusHours(1));

        unreadNotification = new Notification();
        unreadNotification.setId(3L);
        unreadNotification.setUserId(100L);
        unreadNotification.setType("TEAM_INVITE_SENT");
        unreadNotification.setMessage("You have been invited to join startup #1 as CTO");
        unreadNotification.setRead(false);
        unreadNotification.setCreatedAt(LocalDateTime.now());
    }

    // --- createNotification tests ---

    @Test
    @DisplayName("createNotification - success")
    void createNotification_Success() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification1);

        NotificationResponseDTO result = notificationService.createNotification(
                100L, "STARTUP_CREATED", "New startup created in Tech industry");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(100L);
        assertThat(result.getType()).isEqualTo("STARTUP_CREATED");
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("createNotificationFallback - returns DTO without saving")
    void createNotificationFallback_ReturnsDTOWithoutSaving() {
        NotificationResponseDTO result = notificationService.createNotificationFallback(
                100L, "STARTUP_CREATED", "Test message", new RuntimeException("DB down"));

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(100L);
        assertThat(result.getType()).isEqualTo("STARTUP_CREATED");
        assertThat(result.getId()).isNull();
        verify(notificationRepository, never()).save(any());
    }

    // --- getNotificationsByUser tests ---

    @Test
    @DisplayName("getNotificationsByUser - returns all notifications")
    void getNotificationsByUser_ReturnsAll() {
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(100L))
                .thenReturn(Arrays.asList(unreadNotification, notification2, notification1));

        List<NotificationResponseDTO> result = notificationService.getNotificationsByUser(100L);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getType()).isEqualTo("TEAM_INVITE_SENT");
    }

    @Test
    @DisplayName("getNotificationsByUser - returns empty for unknown user")
    void getNotificationsByUser_EmptyForUnknownUser() {
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(999L))
                .thenReturn(List.of());

        List<NotificationResponseDTO> result = notificationService.getNotificationsByUser(999L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getNotificationsByUserFallback - returns empty list")
    void getNotificationsByUserFallback_ReturnsEmpty() {
        List<NotificationResponseDTO> result = notificationService.getNotificationsByUserFallback(
                100L, new RuntimeException("fail"));

        assertThat(result).isEmpty();
    }

    // --- getUnreadNotifications tests ---

    @Test
    @DisplayName("getUnreadNotifications - returns only unread")
    void getUnreadNotifications_ReturnsOnlyUnread() {
        when(notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(100L))
                .thenReturn(List.of(unreadNotification));

        List<NotificationResponseDTO> result = notificationService.getUnreadNotifications(100L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isRead()).isFalse();
        assertThat(result.get(0).getType()).isEqualTo("TEAM_INVITE_SENT");
    }

    @Test
    @DisplayName("getUnreadNotificationsFallback - returns empty list")
    void getUnreadNotificationsFallback_ReturnsEmpty() {
        List<NotificationResponseDTO> result = notificationService.getUnreadNotificationsFallback(
                100L, new RuntimeException("fail"));

        assertThat(result).isEmpty();
    }

    // --- markAsRead tests ---

    @Test
    @DisplayName("markAsRead - success")
    void markAsRead_Success() {
        Notification unread = new Notification();
        unread.setId(3L);
        unread.setUserId(100L);
        unread.setType("TEAM_INVITE_SENT");
        unread.setMessage("Invite");
        unread.setRead(false);
        unread.setCreatedAt(LocalDateTime.now());

        Notification readVersion = new Notification();
        readVersion.setId(3L);
        readVersion.setUserId(100L);
        readVersion.setType("TEAM_INVITE_SENT");
        readVersion.setMessage("Invite");
        readVersion.setRead(true);
        readVersion.setCreatedAt(unread.getCreatedAt());

        when(notificationRepository.findById(3L)).thenReturn(Optional.of(unread));
        when(notificationRepository.save(any(Notification.class))).thenReturn(readVersion);

        NotificationResponseDTO result = notificationService.markAsRead(3L);

        assertThat(result).isNotNull();
        assertThat(result.isRead()).isTrue();
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("markAsRead - throws when not found")
    void markAsRead_WhenNotFound_ThrowsException() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(999L))
                .isInstanceOf(NotificationNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("markAsReadFallback - re-throws NotificationNotFoundException")
    void markAsReadFallback_RethrowsNotFound() {
        NotificationNotFoundException ex = new NotificationNotFoundException(999L);

        assertThatThrownBy(() -> notificationService.markAsReadFallback(999L, ex))
                .isInstanceOf(NotificationNotFoundException.class);
    }

    @Test
    @DisplayName("markAsReadFallback - returns partial DTO for other errors")
    void markAsReadFallback_ReturnsPartialDTO() {
        NotificationResponseDTO result = notificationService.markAsReadFallback(
                1L, new RuntimeException("DB error"));

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.isRead()).isTrue();
    }

    // --- notifyAllUsers tests ---

    @Test
    @DisplayName("notifyAllUsers - sends notification to each user")
    void notifyAllUsers_SendsNotificationToAllUsers() {
        UserDTO user1 = new UserDTO(1L, "Alice", "alice@test.com", "INVESTOR", null, null, null, null);
        UserDTO user2 = new UserDTO(2L, "Bob", "bob@test.com", "FOUNDER", null, null, null, null);
        when(userServiceClient.getAllUsers()).thenReturn(List.of(user1, user2));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification1);

        notificationService.notifyAllUsers("STARTUP_CREATED", "New startup!");

        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    @DisplayName("notifyAllUsers - throws RuntimeException when userService fails")
    void notifyAllUsers_ThrowsWhenUserServiceFails() {
        when(userServiceClient.getAllUsers()).thenThrow(new RuntimeException("Service unavailable"));

        assertThatThrownBy(() -> notificationService.notifyAllUsers("TYPE", "msg"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to notify users");
    }

    @Test
    @DisplayName("notifyAllUsersFallback - logs without throwing or saving")
    void notifyAllUsersFallback_LogsWithoutThrowing() {
        notificationService.notifyAllUsersFallback("TYPE", "msg", new RuntimeException("fail"));

        verifyNoInteractions(notificationRepository);
    }

    // --- sendStartupCreatedEmailToAllInvestors tests ---

    @Test
    @DisplayName("sendStartupCreatedEmailToAllInvestors - sends bulk email only to investors")
    void sendStartupCreatedEmailToAllInvestors_SendsBulkEmail() {
        UserDTO investor1 = new UserDTO(1L, "Alice", "alice@test.com", "INVESTOR", null, null, null, null);
        UserDTO investor2 = new UserDTO(2L, "Bob", "bob@test.com", "INVESTOR", null, null, null, null);
        when(userServiceClient.getUsersByRole("INVESTOR")).thenReturn(List.of(investor1, investor2));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification1);

        notificationService.sendStartupCreatedEmailToAllInvestors(1L, "TechStartup", "Tech", 500000.0);

        verify(userServiceClient).getUsersByRole("INVESTOR");
        verify(userServiceClient, never()).getAllUsers();
        verify(emailService).sendBulkEmail(any(String[].class), anyString(), anyString());
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    @DisplayName("sendStartupCreatedEmailToAllInvestors - does not notify non-investor users")
    void sendStartupCreatedEmailToAllInvestors_DoesNotNotifyNonInvestors() {
        UserDTO investor = new UserDTO(1L, "Alice", "alice@test.com", "INVESTOR", null, null, null, null);
        when(userServiceClient.getUsersByRole("INVESTOR")).thenReturn(List.of(investor));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification1);

        notificationService.sendStartupCreatedEmailToAllInvestors(1L, "TechStartup", "Tech", 500000.0);

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("sendStartupCreatedEmailToAllInvestors - handles empty investor list")
    void sendStartupCreatedEmailToAllInvestors_NoUsers() {
        when(userServiceClient.getUsersByRole("INVESTOR")).thenReturn(List.of());

        notificationService.sendStartupCreatedEmailToAllInvestors(1L, "TechStartup", "Tech", 500000.0);

        verify(emailService).sendBulkEmail(any(String[].class), anyString(), anyString());
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    // --- sendInvestmentInterestEmailToFounder tests ---

    @Test
    @DisplayName("sendInvestmentInterestEmailToFounder - sends email and notification to founder")
    void sendInvestmentInterestEmailToFounder_SendsEmail() {
        StartupDTO startup = new StartupDTO(1L, "TechStartup", null, null, null, null, null, 100L);
        UserDTO founder = new UserDTO(100L, "Alice", "alice@test.com", "FOUNDER", null, null, null, null);
        UserDTO investor = new UserDTO(200L, "Bob", "bob@test.com", "INVESTOR", null, null, null, null);
        when(startupServiceClient.getStartupById(1L)).thenReturn(startup);
        when(userServiceClient.getUserById(100L)).thenReturn(founder);
        when(userServiceClient.getUserById(200L)).thenReturn(investor);
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification1);

        notificationService.sendInvestmentInterestEmailToFounder(1L, 200L, "Bob");

        verify(emailService).sendEmail(eq("alice@test.com"), anyString(), anyString());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("sendInvestmentInterestEmailToFounder - skips when founder is null")
    void sendInvestmentInterestEmailToFounder_SkipsWhenInvestorNotFound() {
        StartupDTO startup = new StartupDTO(1L, "TechStartup", null, null, null, null, null, 100L);
        when(startupServiceClient.getStartupById(1L)).thenReturn(startup);
        when(userServiceClient.getUserById(100L)).thenReturn(null);
        when(userServiceClient.getUserById(200L)).thenReturn(
                new UserDTO(200L, "Bob", "bob@test.com", "INVESTOR", null, null, null, null));

        notificationService.sendInvestmentInterestEmailToFounder(1L, 200L, "Bob");

        verify(emailService, never()).sendEmail(any(), any(), any());
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    // --- sendInvestmentCreatedEmailToFounder tests ---

    @Test
    @DisplayName("sendInvestmentCreatedEmailToFounder - sends email and notification to founder")
    void sendInvestmentCreatedEmailToFounder_SendsEmailToFounder() {
        UserDTO founder = new UserDTO(100L, "Alice", "alice@test.com", "FOUNDER", null, null, null, null);
        UserDTO investor = new UserDTO(200L, "Bob", "bob@test.com", "INVESTOR", null, null, null, null);
        when(userServiceClient.getUserById(100L)).thenReturn(founder);
        when(userServiceClient.getUserById(200L)).thenReturn(investor);
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification1);

        notificationService.sendInvestmentCreatedEmailToFounder(1L, 100L, 200L, 50000.0);

        verify(emailService).sendEmail(eq("alice@test.com"), anyString(), anyString());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("sendInvestmentCreatedEmailToFounder - skips when founder is null")
    void sendInvestmentCreatedEmailToFounder_SkipsWhenFounderNotFound() {
        when(userServiceClient.getUserById(100L)).thenReturn(null);
        when(userServiceClient.getUserById(200L)).thenReturn(
                new UserDTO(200L, "Bob", "bob@test.com", "INVESTOR", null, null, null, null));

        notificationService.sendInvestmentCreatedEmailToFounder(1L, 100L, 200L, 50000.0);

        verify(emailService, never()).sendEmail(any(), any(), any());
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    // --- sendTeamInviteEmail tests ---

    @Test
    @DisplayName("sendTeamInviteEmail - sends email to invited user")
    void sendTeamInviteEmail_SendsEmail() {
        UserDTO invitedUser = new UserDTO(300L, "Charlie", "charlie@test.com", "COFOUNDER", null, null, null, null);
        when(userServiceClient.getUserById(300L)).thenReturn(invitedUser);

        notificationService.sendTeamInviteEmail(1L, 300L, "CTO");

        verify(emailService).sendEmail(eq("charlie@test.com"), anyString(), anyString());
    }

    @Test
    @DisplayName("sendTeamInviteEmail - skips when invited user has no email")
    void sendTeamInviteEmail_SkipsWhenUserHasNoEmail() {
        when(userServiceClient.getUserById(300L)).thenReturn(new UserDTO(300L, "Charlie", null, "COFOUNDER", null, null, null, null));

        notificationService.sendTeamInviteEmail(1L, 300L, "CTO");

        verify(emailService, never()).sendEmail(any(), any(), any());
    }
}
