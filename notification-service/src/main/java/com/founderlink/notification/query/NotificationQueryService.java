package com.founderlink.notification.query;

import com.founderlink.notification.dto.NotificationResponseDTO;
import com.founderlink.notification.repository.NotificationRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationQueryService {

    private static final Logger log = LoggerFactory.getLogger(NotificationQueryService.class);

    private final NotificationRepository notificationRepository;

    public NotificationQueryService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * QUERY: Get all notifications for a user (newest first).
     * Cache key = userId.
     */
    @CircuitBreaker(name = "notificationService", fallbackMethod = "getNotificationsByUserFallback")
    @Retry(name = "notificationService")
    @Cacheable(value = "notificationsByUser", key = "#userId")
    public List<NotificationResponseDTO> getNotificationsByUser(Long userId) {
        log.info("QUERY - getNotificationsByUser: userId={} (cache miss, hitting DB)", userId);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<NotificationResponseDTO> getNotificationsByUserFallback(Long userId, Throwable throwable) {
        log.error("Fallback - getNotificationsByUser. User: {}, Reason: {}", userId, throwable.getMessage());
        return Collections.emptyList();
    }

    /**
     * QUERY: Get only unread notifications for a user.
     * Cache key = userId.
     */
    @CircuitBreaker(name = "notificationService", fallbackMethod = "getUnreadNotificationsFallback")
    @Retry(name = "notificationService")
    @Cacheable(value = "unreadNotifications", key = "#userId")
    public List<NotificationResponseDTO> getUnreadNotifications(Long userId) {
        log.info("QUERY - getUnreadNotifications: userId={} (cache miss, hitting DB)", userId);
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<NotificationResponseDTO> getUnreadNotificationsFallback(Long userId, Throwable throwable) {
        log.error("Fallback - getUnreadNotifications. User: {}, Reason: {}", userId, throwable.getMessage());
        return Collections.emptyList();
    }

    private NotificationResponseDTO mapToDTO(com.founderlink.notification.entity.Notification notification) {
        return NotificationResponseDTO.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .type(notification.getType())
                .message(notification.getMessage())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
