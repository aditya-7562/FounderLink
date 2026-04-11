package com.founderlink.notification.controller;

import com.founderlink.notification.dto.NotificationResponseDTO;
import com.founderlink.notification.exception.GlobalExceptionHandler;
import com.founderlink.notification.exception.NotificationNotFoundException;
import com.founderlink.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private NotificationResponseDTO dto1;
    private NotificationResponseDTO dto2;
    private NotificationResponseDTO unreadDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        dto1 = NotificationResponseDTO.builder()
                .id(1L).userId(100L).type("STARTUP_CREATED")
                .message("New startup").read(true)
                .createdAt(LocalDateTime.now().minusHours(2)).build();

        dto2 = NotificationResponseDTO.builder()
                .id(2L).userId(100L).type("INVESTMENT_CREATED")
                .message("New investment").read(true)
                .createdAt(LocalDateTime.now().minusHours(1)).build();

        unreadDto = NotificationResponseDTO.builder()
                .id(3L).userId(100L).type("TEAM_INVITE_SENT")
                .message("You have been invited").read(false)
                .createdAt(LocalDateTime.now()).build();
    }

    // --- GET /notifications/{userId} ---

    @Test
    @DisplayName("GET /notifications/{userId} - returns all notifications")
    void getNotifications_ReturnsAll() throws Exception {
        when(notificationService.getNotificationsByUser(100L))
                .thenReturn(Arrays.asList(unreadDto, dto2, dto1));

        mockMvc.perform(get("/notifications/100").header("X-User-Id", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].type").value("TEAM_INVITE_SENT"));
    }

    @Test
    @DisplayName("GET /notifications/{userId} - paginated success")
    void getNotifications_Paginated() throws Exception {
        when(notificationService.getNotificationsByUser(eq(100L), any()))
                .thenReturn(new PageImpl<>(List.of(unreadDto)));

        mockMvc.perform(get("/notifications/100")
                        .header("X-User-Id", 100L)
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "type,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    @Test
    @DisplayName("GET /notifications/{userId} - sorting edge cases")
    void getNotifications_SortingEdgeCases() throws Exception {
        when(notificationService.getNotificationsByUser(eq(100L), any()))
                .thenReturn(new PageImpl<>(List.of(unreadDto)));

        // Empty sort string
        mockMvc.perform(get("/notifications/100")
                        .header("X-User-Id", 100L)
                        .param("page", "0")
                        .param("sort", " "))
                .andExpect(status().isOk());

        // Sort with property only
        mockMvc.perform(get("/notifications/100")
                        .header("X-User-Id", 100L)
                        .param("page", "0")
                        .param("sort", "type"))
                .andExpect(status().isOk());

        // Sort with invalid format (comma only) - should handle property tokens[0] logic
        mockMvc.perform(get("/notifications/100")
                        .header("X-User-Id", 100L)
                        .param("page", "0")
                        .param("sort", ","))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /notifications/{userId} - boundary page/size values")
    void getNotifications_BoundaryValues() throws Exception {
        when(notificationService.getNotificationsByUser(eq(100L), any()))
                .thenReturn(new PageImpl<>(List.of(unreadDto)));

        // Negative page, huge size
        mockMvc.perform(get("/notifications/100")
                        .header("X-User-Id", 100L)
                        .param("page", "-1")
                        .param("size", "1000"))
                .andExpect(status().isOk());
    }

    // --- GET /notifications/{userId}/unread ---

    @Test
    @DisplayName("GET /notifications/{userId}/unread - returns only unread")
    void getUnreadNotifications_ReturnsUnread() throws Exception {
        when(notificationService.getUnreadNotifications(100L))
                .thenReturn(List.of(unreadDto));

        mockMvc.perform(get("/notifications/100/unread").header("X-User-Id", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("GET /notifications/{userId}/unread - paginated success")
    void getUnreadNotifications_Paginated() throws Exception {
        when(notificationService.getUnreadNotifications(eq(100L), any()))
                .thenReturn(new PageImpl<>(List.of(unreadDto)));

        mockMvc.perform(get("/notifications/100/unread")
                        .header("X-User-Id", 100L)
                        .param("page", "0"))
                .andExpect(status().isOk());
    }

    // --- Mark as read ---

    @Test
    @DisplayName("PUT /notifications/{id}/read - marks as read")
    void markAsRead_Success() throws Exception {
        when(notificationService.markAsReadForUser(3L, 100L)).thenReturn(unreadDto);

        mockMvc.perform(patch("/notifications/3/read").header("X-User-Id", 100L))
                .andExpect(status().isOk());

        mockMvc.perform(put("/notifications/3/read").header("X-User-Id", 100L))
                .andExpect(status().isOk());
    }

    // --- Security / Exception Handling ---

    @Test
    @DisplayName("GET /notifications/{userId} - forbidden on userId mismatch")
    void verifyOwnership_Forbidden() throws Exception {
        mockMvc.perform(get("/notifications/100").header("X-User-Id", 200L))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /notifications/{id}/read - not found returns 404")
    void markAsRead_NotFound() throws Exception {
        when(notificationService.markAsReadForUser(999L, 100L))
                .thenThrow(new NotificationNotFoundException(999L));

        mockMvc.perform(put("/notifications/999/read").header("X-User-Id", 100L))
                .andExpect(status().isNotFound());
    }
}
