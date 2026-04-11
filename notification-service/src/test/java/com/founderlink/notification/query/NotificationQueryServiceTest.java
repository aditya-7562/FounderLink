package com.founderlink.notification.query;

import com.founderlink.notification.dto.NotificationResponseDTO;
import com.founderlink.notification.entity.Notification;
import com.founderlink.notification.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationQueryServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationQueryService queryService;

    @Test
    @DisplayName("getNotificationsByUser - success (List)")
    void getNotificationsByUser_Success() {
        Notification n = new Notification();
        n.setId(1L);
        n.setUserId(100L);
        n.setCreatedAt(LocalDateTime.now());
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(100L)).thenReturn(List.of(n));

        List<NotificationResponseDTO> result = queryService.getNotificationsByUser(100L);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getNotificationsByUser - success (Page)")
    void getNotificationsByUser_Page_Success() {
        Notification n = new Notification();
        n.setId(1L);
        n.setUserId(100L);
        Pageable p = PageRequest.of(0, 10);
        when(notificationRepository.findByUserId(eq(100L), eq(p))).thenReturn(new PageImpl<>(List.of(n)));

        Page<NotificationResponseDTO> result = queryService.getNotificationsByUser(100L, p);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("getUnreadNotifications - success (List)")
    void getUnreadNotifications_Success() {
        Notification n = new Notification();
        n.setId(1L);
        n.setUserId(100L);
        n.setRead(false);
        when(notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(100L)).thenReturn(List.of(n));

        List<NotificationResponseDTO> result = queryService.getUnreadNotifications(100L);
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("getUnreadNotifications - success (Page)")
    void getUnreadNotifications_Page_Success() {
        Notification n = new Notification();
        n.setId(1L);
        n.setUserId(100L);
        Pageable p = PageRequest.of(0, 10);
        when(notificationRepository.findByUserIdAndReadFalse(eq(100L), eq(p))).thenReturn(new PageImpl<>(List.of(n)));

        Page<NotificationResponseDTO> result = queryService.getUnreadNotifications(100L, p);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("getNotificationsByUserFallback - returns empty")
    void getNotificationsByUserFallback() {
        assertThat(queryService.getNotificationsByUserFallback(100L, new RuntimeException())).isEmpty();
    }

    @Test
    @DisplayName("getUnreadNotificationsFallback - returns empty")
    void getUnreadNotificationsFallback() {
        assertThat(queryService.getUnreadNotificationsFallback(100L, new RuntimeException())).isEmpty();
    }
}
