package com.founderlink.notification.controller;

import com.founderlink.notification.dto.NotificationResponseDTO;
import com.founderlink.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@Slf4j
@RestController
@RequestMapping("/notifications")
@Tag(name = "Notifications", description = "Endpoints for managing notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get all notifications for a user", description = "Fetches all notifications for the specified user.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notifications fetched successfully")
    })
    public ResponseEntity<List<NotificationResponseDTO>> getNotifications(@PathVariable Long userId) {
        log.info("GET /notifications/{} - fetching all notifications", userId);
        return ResponseEntity.ok(notificationService.getNotificationsByUser(userId));
    }

    @GetMapping("/{userId}/unread")
    @Operation(summary = "Get unread notifications for a user", description = "Fetches all unread notifications for the specified user.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Unread notifications fetched successfully")
    })
    public ResponseEntity<List<NotificationResponseDTO>> getUnreadNotifications(@PathVariable Long userId) {
        log.info("GET /notifications/{}/unread - fetching unread notifications", userId);
        return ResponseEntity.ok(notificationService.getUnreadNotifications(userId));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark notification as read", description = "Marks the specified notification as read.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notification marked as read successfully")
    })
    public ResponseEntity<NotificationResponseDTO> markAsRead(@PathVariable Long id) {
        log.info("PUT /notifications/{}/read - marking as read", id);
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }
}
