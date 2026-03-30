package com.founderlink.notification.consumer;

import com.founderlink.notification.dto.*;
import com.founderlink.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventConsumerNewHandlersTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private com.founderlink.notification.service.EmailService emailService;

    @InjectMocks
    private EventConsumer eventConsumer;

    @Test
    @DisplayName("handleTeamMemberAccepted - processes event successfully")
    void handleTeamMemberAccepted_Success() {
        TeamMemberAcceptedEvent event = new TeamMemberAcceptedEvent(
                1L, 101L, 5L, 300L, "CTO"
        );

        doNothing().when(notificationService).sendTeamMemberAcceptedNotification(
                anyLong(), anyLong(), anyLong(), anyString()
        );

        eventConsumer.handleTeamMemberAccepted(event);

        verify(notificationService, times(1)).sendTeamMemberAcceptedNotification(
                eq(101L), eq(5L), eq(300L), eq("CTO")
        );
    }

    @Test
    @DisplayName("handleTeamMemberAccepted - handles exception gracefully")
    void handleTeamMemberAccepted_HandlesException() {
        TeamMemberAcceptedEvent event = new TeamMemberAcceptedEvent(
                1L, 101L, 5L, 300L, "CTO"
        );

        doThrow(new RuntimeException("Service unavailable"))
                .when(notificationService).sendTeamMemberAcceptedNotification(
                        anyLong(), anyLong(), anyLong(), anyString()
                );

        eventConsumer.handleTeamMemberAccepted(event);

        verify(notificationService, times(1)).sendTeamMemberAcceptedNotification(
                anyLong(), anyLong(), anyLong(), anyString()
        );
    }

    @Test
    @DisplayName("handleTeamMemberAcceptedFallback - logs without throwing")
    void handleTeamMemberAcceptedFallback_LogsWithoutThrowing() {
        TeamMemberAcceptedEvent event = new TeamMemberAcceptedEvent(
                1L, 101L, 5L, 300L, "CTO"
        );

        eventConsumer.handleTeamMemberAcceptedFallback(event, new RuntimeException("fail"));

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("handleTeamMemberRejected - processes event successfully")
    void handleTeamMemberRejected_Success() {
        TeamMemberRejectedEvent event = new TeamMemberRejectedEvent(
                2L, 102L, 6L, 301L, "CFO"
        );

        doNothing().when(notificationService).sendTeamMemberRejectedNotification(
                anyLong(), anyLong(), anyLong(), anyString()
        );

        eventConsumer.handleTeamMemberRejected(event);

        verify(notificationService, times(1)).sendTeamMemberRejectedNotification(
                eq(102L), eq(6L), eq(301L), eq("CFO")
        );
    }

    @Test
    @DisplayName("handleTeamMemberRejectedFallback - logs without throwing")
    void handleTeamMemberRejectedFallback_LogsWithoutThrowing() {
        TeamMemberRejectedEvent event = new TeamMemberRejectedEvent(
                2L, 102L, 6L, 301L, "CFO"
        );

        eventConsumer.handleTeamMemberRejectedFallback(event, new RuntimeException("fail"));

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("handlePaymentCompleted - processes event successfully")
    void handlePaymentCompleted_Success() {
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                1L, 100L, 200L, 5L, 101L, new BigDecimal("50000")
        );

        doNothing().when(notificationService).sendPaymentCompletedNotification(
                anyLong(), anyLong(), anyLong()
        );

        eventConsumer.handlePaymentCompleted(event);

        verify(notificationService, times(1)).sendPaymentCompletedNotification(
                eq(1L), eq(200L), eq(5L)
        );
    }

    @Test
    @DisplayName("handlePaymentCompletedFallback - logs without throwing")
    void handlePaymentCompletedFallback_LogsWithoutThrowing() {
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                1L, 100L, 200L, 5L, 101L, new BigDecimal("50000")
        );

        eventConsumer.handlePaymentCompletedFallback(event, new RuntimeException("fail"));

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("handlePaymentFailed - processes event successfully")
    void handlePaymentFailed_Success() {
        PaymentFailedEvent event = new PaymentFailedEvent(
                2L, 101L, 200L, 5L, 100L, new BigDecimal("50000"), "Insufficient funds"
        );

        doNothing().when(notificationService).sendPaymentFailedNotification(
                anyLong(), anyLong(), anyString()
        );

        eventConsumer.handlePaymentFailed(event);

        verify(notificationService, times(1)).sendPaymentFailedNotification(
                eq(2L), eq(200L), eq("Insufficient funds")
        );
    }

    @Test
    @DisplayName("handlePaymentFailedFallback - logs without throwing")
    void handlePaymentFailedFallback_LogsWithoutThrowing() {
        PaymentFailedEvent event = new PaymentFailedEvent(
                2L, 101L, 200L, 5L, 100L, new BigDecimal("50000"), "Card declined"
        );

        eventConsumer.handlePaymentFailedFallback(event, new RuntimeException("fail"));

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("handleInvestmentApproved - processes event successfully")
    void handleInvestmentApproved_Success() {
        InvestmentApprovedEvent event = new InvestmentApprovedEvent(
                1L, 200L, 100L, 101L, new BigDecimal("50000")
        );

        doNothing().when(notificationService).sendInvestmentApprovedNotification(
                anyLong(), anyLong(), anyLong(), anyString()
        );

        eventConsumer.handleInvestmentApproved(event);

        verify(notificationService, times(1)).sendInvestmentApprovedNotification(
                eq(1L), eq(200L), eq(101L), contains("50000")
        );
    }

    @Test
    @DisplayName("handleInvestmentApproved - handles null amount")
    void handleInvestmentApproved_NullAmount() {
        InvestmentApprovedEvent event = new InvestmentApprovedEvent(
                1L, 200L, 100L, 101L, null
        );

        doNothing().when(notificationService).sendInvestmentApprovedNotification(
                anyLong(), anyLong(), anyLong(), anyString()
        );

        eventConsumer.handleInvestmentApproved(event);

        verify(notificationService, times(1)).sendInvestmentApprovedNotification(
                eq(1L), eq(200L), eq(101L), eq("N/A")
        );
    }

    @Test
    @DisplayName("handleInvestmentApprovedFallback - logs without throwing")
    void handleInvestmentApprovedFallback_LogsWithoutThrowing() {
        InvestmentApprovedEvent event = new InvestmentApprovedEvent(
                1L, 200L, 100L, 101L, new BigDecimal("50000")
        );

        eventConsumer.handleInvestmentApprovedFallback(event, new RuntimeException("fail"));

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("handleInvestmentRejected - processes event successfully")
    void handleInvestmentRejected_Success() {
        InvestmentRejectedEvent event = new InvestmentRejectedEvent(
                2L, 201L, 102L, 103L, new BigDecimal("75000"), "Not aligned with goals"
        );

        doNothing().when(notificationService).sendInvestmentRejectedNotification(
                anyLong(), anyLong(), anyLong(), anyString(), anyString()
        );

        eventConsumer.handleInvestmentRejected(event);

        verify(notificationService, times(1)).sendInvestmentRejectedNotification(
                eq(2L), eq(201L), eq(103L), contains("75000"), eq("Not aligned with goals")
        );
    }

    @Test
    @DisplayName("handleInvestmentRejected - handles null amount and reason")
    void handleInvestmentRejected_NullValues() {
        InvestmentRejectedEvent event = new InvestmentRejectedEvent(
                2L, 201L, 102L, 103L, null, null
        );

        doNothing().when(notificationService).sendInvestmentRejectedNotification(
                anyLong(), anyLong(), anyLong(), anyString(), anyString()
        );

        eventConsumer.handleInvestmentRejected(event);

        verify(notificationService, times(1)).sendInvestmentRejectedNotification(
                eq(2L), eq(201L), eq(103L), eq("N/A"), isNull()
        );
    }

    @Test
    @DisplayName("handleInvestmentRejectedFallback - logs without throwing")
    void handleInvestmentRejectedFallback_LogsWithoutThrowing() {
        InvestmentRejectedEvent event = new InvestmentRejectedEvent(
                2L, 201L, 102L, 103L, new BigDecimal("75000"), "Not aligned"
        );

        eventConsumer.handleInvestmentRejectedFallback(event, new RuntimeException("fail"));

        verifyNoInteractions(notificationService);
    }
}
