package com.founderlink.notification.controller;

import com.founderlink.notification.dto.NotificationResponseDTO;
import com.founderlink.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

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
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notifications fetched successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<?> getNotifications(
            @PathVariable Long userId,
            @RequestHeader("X-User-Id") Long authenticatedUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort,
            HttpServletRequest request
    ) {
        verifyOwnershipOrThrow("GET /notifications/{userId}", authenticatedUserId, userId);
        log.info("GET /notifications/{} - fetching all notifications", userId);
        if (!isPaginatedRequest(request)) {
            return ResponseEntity.ok(notificationService.getNotificationsByUser(userId));
        }

        Pageable pageable = buildPageable(page, size, sort, "createdAt", Sort.Direction.DESC);
        return ResponseEntity.ok(toPaginatedResponse(notificationService.getNotificationsByUser(userId, pageable)));
    }

    @GetMapping("/{userId}/unread")
    @Operation(summary = "Get unread notifications for a user", description = "Fetches all unread notifications for the specified user.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Unread notifications fetched successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<?> getUnreadNotifications(
            @PathVariable Long userId,
            @RequestHeader("X-User-Id") Long authenticatedUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort,
            HttpServletRequest request
    ) {
        verifyOwnershipOrThrow("GET /notifications/{userId}/unread", authenticatedUserId, userId);
        log.info("GET /notifications/{}/unread - fetching unread notifications", userId);
        if (!isPaginatedRequest(request)) {
            return ResponseEntity.ok(notificationService.getUnreadNotifications(userId));
        }

        Pageable pageable = buildPageable(page, size, sort, "createdAt", Sort.Direction.DESC);
        return ResponseEntity.ok(toPaginatedResponse(notificationService.getUnreadNotifications(userId, pageable)));
    }

    @RequestMapping(value = "/{id}/read", method = {RequestMethod.PATCH, RequestMethod.PUT})
    @Operation(summary = "Mark notification as read", description = "Marks the specified notification as read.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notification marked as read successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Notification not found")
    })
    public ResponseEntity<NotificationResponseDTO> markAsRead(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long authenticatedUserId,
            HttpServletRequest request
    ) {
        log.info(
                "{} /notifications/{}/read - token.userId={} marking as read",
                request.getMethod(),
                id,
                authenticatedUserId
        );
        return ResponseEntity.ok(notificationService.markAsReadForUser(id, authenticatedUserId));
    }

    private void verifyOwnershipOrThrow(String endpoint, Long tokenUserId, Long pathUserId) {
        log.info(
                "{} ownership check - token.userId={}, path.userId={}",
                endpoint,
                tokenUserId,
                pathUserId
        );
        if (!tokenUserId.equals(pathUserId)) {
            log.warn(
                    "{} forbidden - token.userId={} does not match path.userId={}",
                    endpoint,
                    tokenUserId,
                    pathUserId
            );
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Requested userId does not match authenticated user.");
        }
    }

    private boolean isPaginatedRequest(HttpServletRequest request) {
        return request.getParameterMap().containsKey("page");
    }

    private Pageable buildPageable(int page, int size, String sort, String defaultProperty, Sort.Direction defaultDirection) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Sort resolvedSort = resolveSort(sort, defaultProperty, defaultDirection);
        return PageRequest.of(safePage, safeSize, resolvedSort);
    }

    private Sort resolveSort(String sort, String defaultProperty, Sort.Direction defaultDirection) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(defaultDirection, defaultProperty);
        }

        String[] tokens = sort.split(",");
        String property = tokens[0].isBlank() ? defaultProperty : tokens[0].trim();
        Sort.Direction direction = tokens.length > 1 && "desc".equalsIgnoreCase(tokens[1].trim())
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direction, property);
    }

    private <T> Map<String, Object> toPaginatedResponse(Page<T> page) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("content", page.getContent());
        payload.put("page", page.getNumber());
        payload.put("size", page.getSize());
        payload.put("totalElements", page.getTotalElements());
        payload.put("totalPages", page.getTotalPages());
        payload.put("last", page.isLast());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", payload);
        response.put("error", null);
        return response;
    }
}
