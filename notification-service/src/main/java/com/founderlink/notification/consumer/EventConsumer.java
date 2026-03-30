package com.founderlink.notification.consumer;

import com.founderlink.notification.dto.*;
import com.founderlink.notification.service.EmailService;
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
    private final EmailService emailService;

    public EventConsumer(NotificationService notificationService, EmailService emailService) {
        this.notificationService = notificationService;
        this.emailService = emailService;
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

    @RabbitListener(queues = "password-reset-queue")
    @CircuitBreaker(name = "notificationService", fallbackMethod = "handlePasswordResetFallback")
    @Retry(name = "notificationService")
    public void handlePasswordResetEvent(PasswordResetEmailEvent event) {
        logger.info("Received PASSWORD_RESET event for email: {}", event.getEmail());

        try {
            emailService.sendPasswordResetPinEmail(event.getEmail(), event.getUserName(), event.getPin());
            logger.info("Password reset PIN email sent to: {}", event.getEmail());
        } catch (Exception e) {
            logger.error("Error processing password reset event for email {}: {}", event.getEmail(), e.getMessage());
        }
    }

    public void handlePasswordResetFallback(PasswordResetEmailEvent event, Throwable throwable) {
        logger.error("Fallback: Failed to send password reset email to {}. Reason: {}",
                event.getEmail(), throwable.getMessage());
    }

    @RabbitListener(queues = "team.accepted.queue")
    @CircuitBreaker(name = "notificationService", fallbackMethod = "handleTeamMemberAcceptedFallback")
    @Retry(name = "notificationService")
    public void handleTeamMemberAccepted(TeamMemberAcceptedEvent event) {
        logger.info("Received TEAM_MEMBER_ACCEPTED event: {}", event);

        try {
            notificationService.sendTeamMemberAcceptedNotification(
                    event.getStartupId(),
                    event.getFounderId(),
                    event.getAcceptedUserId(),
                    event.getRole()
            );
            logger.info("Team member accepted notification sent to founder {}", event.getFounderId());
        } catch (Exception e) {
            logger.error("Error processing team member accepted event: {}", e.getMessage());
        }
    }

    public void handleTeamMemberAcceptedFallback(TeamMemberAcceptedEvent event, Throwable throwable) {
        logger.error("Fallback: Failed to process TEAM_MEMBER_ACCEPTED event: {}. Reason: {}",
                event, throwable.getMessage());
    }

    @RabbitListener(queues = "team.rejected.queue")
    @CircuitBreaker(name = "notificationService", fallbackMethod = "handleTeamMemberRejectedFallback")
    @Retry(name = "notificationService")
    public void handleTeamMemberRejected(TeamMemberRejectedEvent event) {
        logger.info("Received TEAM_MEMBER_REJECTED event: {}", event);

        try {
            notificationService.sendTeamMemberRejectedNotification(
                    event.getStartupId(),
                    event.getFounderId(),
                    event.getRejectedUserId(),
                    event.getRole()
            );
            logger.info("Team member rejected notification sent to founder {}", event.getFounderId());
        } catch (Exception e) {
            logger.error("Error processing team member rejected event: {}", e.getMessage());
        }
    }

    public void handleTeamMemberRejectedFallback(TeamMemberRejectedEvent event, Throwable throwable) {
        logger.error("Fallback: Failed to process TEAM_MEMBER_REJECTED event: {}. Reason: {}",
                event, throwable.getMessage());
    }

    @RabbitListener(queues = "payment.completed.queue")
    @CircuitBreaker(name = "notificationService", fallbackMethod = "handlePaymentCompletedFallback")
    @Retry(name = "notificationService")
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        logger.info("Received PAYMENT_COMPLETED event: {}", event);

        try {
            notificationService.sendPaymentCompletedNotification(
                    event.getInvestmentId(),
                    event.getInvestorId(),
                    event.getFounderId()
            );
            logger.info("Payment completed notifications sent for investment {}", event.getInvestmentId());
        } catch (Exception e) {
            logger.error("Error processing payment completed event: {}", e.getMessage());
        }
    }

    public void handlePaymentCompletedFallback(PaymentCompletedEvent event, Throwable throwable) {
        logger.error("Fallback: Failed to process PAYMENT_COMPLETED event: {}. Reason: {}",
                event, throwable.getMessage());
    }

    @RabbitListener(queues = "payment.failed.queue")
    @CircuitBreaker(name = "notificationService", fallbackMethod = "handlePaymentFailedFallback")
    @Retry(name = "notificationService")
    public void handlePaymentFailed(PaymentFailedEvent event) {
        logger.info("Received PAYMENT_FAILED event: {}", event);

        try {
            notificationService.sendPaymentFailedNotification(
                    event.getInvestmentId(),
                    event.getInvestorId(),
                    event.getReason()
            );
            logger.info("Payment failed notification sent for investment {}", event.getInvestmentId());
        } catch (Exception e) {
            logger.error("Error processing payment failed event: {}", e.getMessage());
        }
    }

    public void handlePaymentFailedFallback(PaymentFailedEvent event, Throwable throwable) {
        logger.error("Fallback: Failed to process PAYMENT_FAILED event: {}. Reason: {}",
                event, throwable.getMessage());
    }

    @RabbitListener(queues = "investment.approved.queue")
    @CircuitBreaker(name = "notificationService", fallbackMethod = "handleInvestmentApprovedFallback")
    @Retry(name = "notificationService")
    public void handleInvestmentApproved(InvestmentApprovedEvent event) {
        logger.info("Received INVESTMENT_APPROVED event: {}", event);

        try {
            notificationService.sendInvestmentApprovedNotification(
                    event.getInvestmentId(),
                    event.getInvestorId(),
                    event.getStartupId(),
                    event.getAmount() != null ? "$" + event.getAmount().toString() : "N/A"
            );
            logger.info("Investment approved notification sent to investor {}", event.getInvestorId());
        } catch (Exception e) {
            logger.error("Error processing investment approved event: {}", e.getMessage());
        }
    }

    public void handleInvestmentApprovedFallback(InvestmentApprovedEvent event, Throwable throwable) {
        logger.error("Fallback: Failed to process INVESTMENT_APPROVED event: {}. Reason: {}",
                event, throwable.getMessage());
    }

    @RabbitListener(queues = "investment.rejected.queue")
    @CircuitBreaker(name = "notificationService", fallbackMethod = "handleInvestmentRejectedFallback")
    @Retry(name = "notificationService")
    public void handleInvestmentRejected(InvestmentRejectedEvent event) {
        logger.info("Received INVESTMENT_REJECTED event: {}", event);

        try {
            notificationService.sendInvestmentRejectedNotification(
                    event.getInvestmentId(),
                    event.getInvestorId(),
                    event.getStartupId(),
                    event.getAmount() != null ? "$" + event.getAmount().toString() : "N/A",
                    event.getRejectionReason()
            );
            logger.info("Investment rejected notification sent to investor {}", event.getInvestorId());
        } catch (Exception e) {
            logger.error("Error processing investment rejected event: {}", e.getMessage());
        }
    }

    public void handleInvestmentRejectedFallback(InvestmentRejectedEvent event, Throwable throwable) {
        logger.error("Fallback: Failed to process INVESTMENT_REJECTED event: {}. Reason: {}",
                event, throwable.getMessage());
    }
}
