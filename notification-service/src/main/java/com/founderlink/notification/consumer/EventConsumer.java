package com.founderlink.notification.consumer;

import com.founderlink.notification.service.NotificationService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class EventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);

    private final NotificationService notificationService;

    public EventConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = "startup.queue")
    @CircuitBreaker(name = "notificationService", fallbackMethod = "handleStartupCreatedFallback")
    @Retry(name = "notificationService")
    public void handleStartupCreated(Map<String, Object> event) {
        logger.info("Received STARTUP_CREATED event: {}", event);

        try {
            Long startupId = Long.valueOf(event.get("startupId").toString());
            String startupName = event.get("startupName") != null ? event.get("startupName").toString() : "New Startup";
            String industry = event.get("industry").toString();
            Double fundingGoal = Double.valueOf(event.get("fundingGoal").toString());

            // Send email to all investors about the new startup
            notificationService.sendStartupCreatedEmailToAllInvestors(startupId, startupName, industry, fundingGoal);

            logger.info("Startup created emails sent for startup #{}", startupId);
        } catch (Exception e) {
            logger.error("Error processing startup created event: {}", e.getMessage());
        }
    }

    public void handleStartupCreatedFallback(Map<String, Object> event, Throwable throwable) {
        logger.error("Fallback: Failed to process STARTUP_CREATED event: {}. Reason: {}",
                event, throwable.getMessage());
    }

    @RabbitListener(queues = "investment.queue")
    @CircuitBreaker(name = "notificationService", fallbackMethod = "handleInvestmentCreatedFallback")
    @Retry(name = "notificationService")
    public void handleInvestmentCreated(Map<String, Object> event) {
        logger.info("Received INVESTMENT_CREATED event: {}", event);

        try {
            Long investorId = Long.valueOf(event.get("investorId").toString());
            Long startupId = Long.valueOf(event.get("startupId").toString());
            Double amount = Double.valueOf(event.get("amount").toString());
            Long founderId = Long.valueOf(event.get("founderId").toString());

            // Send email to founder about the investment
            notificationService.sendInvestmentCreatedEmailToFounder(startupId, founderId, investorId, amount);

            logger.info("Investment created email sent to founder {} for startup {}", founderId, startupId);
        } catch (Exception e) {
            logger.error("Error processing investment created event: {}", e.getMessage());
        }
    }

    public void handleInvestmentCreatedFallback(Map<String, Object> event, Throwable throwable) {
        logger.error("Fallback: Failed to process INVESTMENT_CREATED event: {}. Reason: {}",
                event, throwable.getMessage());
    }

    @RabbitListener(queues = "team.queue")
    @CircuitBreaker(name = "notificationService", fallbackMethod = "handleTeamInviteFallback")
    @Retry(name = "notificationService")
    public void handleTeamInvite(Map<String, Object> event) {
        logger.info("Received TEAM_INVITE_SENT event: {}", event);

        try {
            Long invitedUserId = Long.valueOf(event.get("invitedUserId").toString());
            Long startupId = Long.valueOf(event.get("startupId").toString());
            String role = event.get("role").toString();

            String message = String.format("You have been invited to join startup #%d as %s",
                    startupId, role);

            notificationService.createNotification(invitedUserId, "TEAM_INVITE_SENT", message);
            notificationService.sendTeamInviteEmail(startupId, invitedUserId, role);

            logger.info("Notification and email sent for user {}: {}", invitedUserId, message);
        } catch (Exception e) {
            logger.error("Error processing team invite event: {}", e.getMessage());
        }
    }

    public void handleTeamInviteFallback(Map<String, Object> event, Throwable throwable) {
        logger.error("Fallback: Failed to process TEAM_INVITE_SENT event: {}. Reason: {}",
                event, throwable.getMessage());
    }

    @RabbitListener(queues = "messaging.queue")
    @CircuitBreaker(name = "notificationService", fallbackMethod = "handleMessageSentFallback")
    @Retry(name = "notificationService")
    public void handleMessageSent(Map<String, Object> event) {
        logger.info("Received MESSAGE_SENT event: {}", event);

        try {
            Long receiverId = Long.valueOf(event.get("receiverId").toString());
            Long senderId = Long.valueOf(event.get("senderId").toString());
            String senderName = event.get("senderName") != null ? event.get("senderName").toString() : "Someone";

            String message = String.format("You have a new message from %s", senderName);

            notificationService.createNotification(receiverId, "MESSAGE_RECEIVED", message);

            logger.info("In-app notification created for user {} about message from user {}", receiverId, senderId);
        } catch (Exception e) {
            logger.error("Error processing message sent event: {}", e.getMessage());
        }
    }

    public void handleMessageSentFallback(Map<String, Object> event, Throwable throwable) {
        logger.error("Fallback: Failed to process MESSAGE_SENT event: {}. Reason: {}",
                event, throwable.getMessage());
    }
}
