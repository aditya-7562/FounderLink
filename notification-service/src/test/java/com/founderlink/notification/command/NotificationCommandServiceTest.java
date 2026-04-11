package com.founderlink.notification.command;

import com.founderlink.notification.dto.NotificationResponseDTO;
import com.founderlink.notification.entity.Notification;
import com.founderlink.notification.exception.NotificationNotFoundException;
import com.founderlink.notification.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationCommandServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationCommandService commandService;

    @Test
    @DisplayName("createNotification - success")
    void createNotification_Success() {
        Notification n = new Notification();
        n.setId(1L);
        n.setUserId(100L);
        when(notificationRepository.save(any())).thenReturn(n);

        NotificationResponseDTO result = commandService.createNotification(100L, "T", "M");
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("markAsRead - success")
    void markAsRead_Success() {
        Notification n = new Notification();
        n.setId(1L);
        n.setUserId(100L);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));
        when(notificationRepository.save(any())).thenReturn(n);

        NotificationResponseDTO result = commandService.markAsRead(1L);
        assertThat(result.isRead()).isTrue();
    }

    @Test
    @DisplayName("markAsReadForUser - success")
    void markAsReadForUser_Success() {
        Notification n = new Notification();
        n.setId(1L);
        n.setUserId(100L);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));
        when(notificationRepository.save(any())).thenReturn(n);

        NotificationResponseDTO result = commandService.markAsReadForUser(1L, 100L);
        assertThat(result.isRead()).isTrue();
    }

    @Test
    @DisplayName("markAsRead - not found")
    void markAsRead_NotFound() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> commandService.markAsRead(1L)).isInstanceOf(NotificationNotFoundException.class);
    }

    @Test
    @DisplayName("createNotificationFallback - returns DTO")
    void createNotificationFallback() {
        NotificationResponseDTO result = commandService.createNotificationFallback(100L, "T", "M", new RuntimeException("E"));
        assertThat(result.getUserId()).isEqualTo(100L);
        assertThat(result.isRead()).isFalse();
    }

    @Test
    @DisplayName("markAsReadForUser - forbidden on mismatch")
    void markAsReadForUser_Forbidden() {
        Notification n = new Notification();
        n.setId(1L);
        n.setUserId(100L);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));

        assertThatThrownBy(() -> commandService.markAsReadForUser(1L, 200L))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("markAsReadFallback - branch coverage")
    void markAsReadFallback() {
        // Rethrow NotFound
        NotificationNotFoundException ex = new NotificationNotFoundException(1L);
        assertThatThrownBy(() -> commandService.markAsReadFallback(1L, ex)).isEqualTo(ex);

        // Return DTO for others
        NotificationResponseDTO result = commandService.markAsReadFallback(1L, new RuntimeException("E"));
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.isRead()).isTrue();
    }

    @Test
    @DisplayName("markAsReadForUserFallback - branch coverage")
    void markAsReadForUserFallback() {
        // Rethrow NotFound
        NotificationNotFoundException ex = new NotificationNotFoundException(1L);
        assertThatThrownBy(() -> commandService.markAsReadForUserFallback(1L, 100L, ex)).isEqualTo(ex);

        // Rethrow ResponseStatusException
        ResponseStatusException rex = new ResponseStatusException(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> commandService.markAsReadForUserFallback(1L, 100L, rex)).isEqualTo(rex);

        // Return DTO for others
        NotificationResponseDTO result = commandService.markAsReadForUserFallback(1L, 100L, new RuntimeException("E"));
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(100L);
    }
}
