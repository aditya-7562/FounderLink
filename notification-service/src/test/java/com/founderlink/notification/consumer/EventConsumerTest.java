package com.founderlink.notification.consumer;

import com.founderlink.notification.dto.NotificationResponseDTO;
import com.founderlink.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventConsumerTest {

    @Mock
    private NotificationService notificationService;

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
    @DisplayName("handleStartupCreatedFallback - logs without throwing")
    void handleStartupCreatedFallback_LogsWithoutThrowing() {
        Map<String, Object> event = new HashMap<>();
        event.put("founderId", 100);

        // Should not throw
        eventConsumer.handleStartupCreatedFallback(event, new RuntimeException("DB down"));

        verify(notificationService, never()).createNotification(any(), any(), any());
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
    @DisplayName("handleInvestmentCreatedFallback - logs without throwing")
    void handleInvestmentCreatedFallback_LogsWithoutThrowing() {
        Map<String, Object> event = new HashMap<>();
        event.put("investorId", 200);

        eventConsumer.handleInvestmentCreatedFallback(event, new RuntimeException("timeout"));

        verify(notificationService, never()).createNotification(any(), any(), any());
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

        verify(notificationService).createNotification(
                eq(300L), eq("TEAM_INVITE_SENT"), contains("CTO"));
        verify(notificationService).sendTeamInviteEmail(1L, 300L, "CTO");
    }

    @Test
    @DisplayName("handleTeamInviteFallback - logs without throwing")
    void handleTeamInviteFallback_LogsWithoutThrowing() {
        Map<String, Object> event = new HashMap<>();
        event.put("invitedUserId", 300);

        eventConsumer.handleTeamInviteFallback(event, new RuntimeException("connection lost"));

        verify(notificationService, never()).createNotification(any(), any(), any());
    }
}
