package com.founderlink.notification.service;

import com.founderlink.notification.client.StartupServiceClient;
import com.founderlink.notification.client.UserServiceClient;
import com.founderlink.notification.command.NotificationCommandService;
import com.founderlink.notification.dto.NotificationResponseDTO;
import com.founderlink.notification.dto.StartupDTO;
import com.founderlink.notification.dto.UserDTO;
import com.founderlink.notification.query.NotificationQueryService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Facade that preserves the existing NotificationService contract.
 * Delegates writes → NotificationCommandService (CQRS Command side)
 * Delegates reads  → NotificationQueryService   (CQRS Query side + Redis cache)
 * Email helpers remain here — they are event-driven side effects, not cached.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationCommandService commandService;
    private final NotificationQueryService   queryService;
    private final UserServiceClient          userServiceClient;
    private final StartupServiceClient       startupServiceClient;
    private final EmailService               emailService;

    public NotificationService(NotificationCommandService commandService,
                                NotificationQueryService queryService,
                                UserServiceClient userServiceClient,
                                StartupServiceClient startupServiceClient,
                                EmailService emailService) {
        this.commandService       = commandService;
        this.queryService         = queryService;
        this.userServiceClient    = userServiceClient;
        this.startupServiceClient = startupServiceClient;
        this.emailService         = emailService;
    }

    // ── Delegated to Command side ────────────────────────────────────────────

    public NotificationResponseDTO createNotification(Long userId, String type, String message) {
        return commandService.createNotification(userId, type, message);
    }

    public NotificationResponseDTO markAsRead(Long id) {
        return commandService.markAsRead(id);
    }

    // ── Delegated to Query side ──────────────────────────────────────────────

    public List<NotificationResponseDTO> getNotificationsByUser(Long userId) {
        return queryService.getNotificationsByUser(userId);
    }

    public List<NotificationResponseDTO> getUnreadNotifications(Long userId) {
        return queryService.getUnreadNotifications(userId);
    }

    // ── Event-driven email helpers (unchanged) ───────────────────────────────

    @CircuitBreaker(name = "notificationService", fallbackMethod = "notifyAllUsersFallback")
    @Retry(name = "notificationService")
    public void notifyAllUsers(String type, String message) {
        try {
            var users = userServiceClient.getAllUsers();
            log.info("Notifying {} users about event type: {}", users.size(), type);
            users.forEach(user -> {
                try {
                    createNotification(user.getId(), type, message);
                } catch (Exception e) {
                    log.error("Failed to create notification for user {}: {}", user.getId(), e.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Error fetching users from User-Service: {}", e.getMessage());
            throw new RuntimeException("Failed to notify users", e);
        }
    }

    public void notifyAllUsersFallback(String type, String message, Throwable throwable) {
        log.error("Fallback - notifyAllUsers. Type: {}, Reason: {}", type, throwable.getMessage());
    }

    public void sendStartupCreatedEmailToAllInvestors(Long startupId, String startupName, String industry, Double fundingGoal) {
        try {
            List<UserDTO> investors = userServiceClient.getUsersByRole("INVESTOR");
            log.info("Sending startup created email to {} investors", investors.size());

            String notificationMessage = String.format(
                    "New startup '%s' in %s is now open for investment. Funding goal: $%,.2f",
                    startupName, industry, fundingGoal);

            investors.forEach(user -> {
                try { createNotification(user.getId(), "STARTUP_CREATED", notificationMessage); }
                catch (Exception e) { log.error("Failed to create startup notification for user {}: {}", user.getId(), e.getMessage()); }
            });

            String subject = "🚀 New Startup Alert: " + startupName;
            String body = String.format(
                    "Hi Investor,\n\nA new startup has been created that might interest you!\n\n" +
                    "Startup Name: %s\nIndustry: %s\nFunding Goal: $%,.2f\nStartup ID: %d\n\n" +
                    "Check it out and consider investing!\n\nBest regards,\nFounderLink Team",
                    startupName, industry, fundingGoal, startupId);

            String[] emails = investors.stream()
                    .map(UserDTO::getEmail)
                    .filter(email -> email != null && !email.isEmpty())
                    .toArray(String[]::new);

            emailService.sendBulkEmail(emails, subject, body);
        } catch (Exception e) {
            log.error("Error sending startup created emails: {}", e.getMessage());
        }
    }

    public void sendInvestmentInterestEmailToFounder(Long startupId, Long investorId, String investorName) {
        try {
            StartupDTO startup = startupServiceClient.getStartupById(startupId);
            if (startup == null || startup.getFounderId() == null) {
                log.warn("Startup {} has no founder information", startupId);
                return;
            }

            UserDTO founder  = userServiceClient.getUserById(startup.getFounderId());
            UserDTO investor = userServiceClient.getUserById(investorId);

            if (founder == null || founder.getEmail() == null) {
                log.warn("Founder {} has no email", startup.getFounderId());
                return;
            }

            String resolvedName = (investorName != null && !investorName.isBlank()) ? investorName
                    : (investor != null ? investor.getName() : "An investor");

            createNotification(startup.getFounderId(), "INVESTMENT_INTERESTED",
                    String.format("%s is interested in your startup #%d", resolvedName, startupId));

            emailService.sendEmail(founder.getEmail(),
                    "💡 Investor Interest in Your Startup #" + startupId,
                    String.format("Hi Founder,\n\nAn investor is interested in your startup!\n\n" +
                            "Investor Name: %s\nInvestor Email: %s\nStartup ID: %d\n\nBest regards,\nFounderLink Team",
                            resolvedName, investor != null ? investor.getEmail() : "Not available", startupId));
        } catch (Exception e) {
            log.error("Error sending investment interest email: {}", e.getMessage());
        }
    }

    public void sendInvestmentCreatedEmailToFounder(Long startupId, Long founderId, Long investorId, Double amount) {
        try {
            UserDTO founder  = userServiceClient.getUserById(founderId);
            UserDTO investor = userServiceClient.getUserById(investorId);

            if (founder == null || founder.getEmail() == null) {
                log.warn("Founder {} has no email", founderId);
                return;
            }

            String investorName = investor != null ? investor.getName() : "Unknown Investor";
            createNotification(founderId, "INVESTMENT_CREATED",
                    String.format("%s showed investment interest in startup #%d with $%,.2f", investorName, startupId, amount));

            emailService.sendEmail(founder.getEmail(),
                    "💡 New Investor Interest in Startup #" + startupId,
                    String.format("Hi Founder,\n\nAn investor has shown interest in your startup.\n\n" +
                            "Investor Name: %s\nInterested Amount: $%,.2f\nStartup ID: %d\n\nBest regards,\nFounderLink Team",
                            investorName, amount, startupId));
        } catch (Exception e) {
            log.error("Error sending investment created email: {}", e.getMessage());
        }
    }

    public void sendTeamInviteEmail(Long startupId, Long invitedUserId, String role) {
        try {
            UserDTO invitedUser = userServiceClient.getUserById(invitedUserId);
            if (invitedUser == null || invitedUser.getEmail() == null) {
                log.warn("Invited user {} has no email", invitedUserId);
                return;
            }

            emailService.sendEmail(invitedUser.getEmail(),
                    "🤝 Team Invitation for Startup #" + startupId,
                    String.format("Hi %s,\n\nYou have been invited to join startup #%d as %s.\n\n" +
                            "Please log in to FounderLink to review the invitation.\n\nBest regards,\nFounderLink Team",
                            invitedUser.getName() != null ? invitedUser.getName() : "User", startupId, role));
        } catch (Exception e) {
            log.error("Error sending team invite email: {}", e.getMessage());
        }
    }

    public void sendTeamMemberAcceptedNotification(Long startupId, Long founderId, Long acceptedUserId, String role) {
        try {
            UserDTO founder = userServiceClient.getUserById(founderId);
            UserDTO acceptedUser = userServiceClient.getUserById(acceptedUserId);

            if (founder == null || founder.getEmail() == null) {
                log.warn("Founder {} has no email", founderId);
                return;
            }

            String memberName = acceptedUser != null ? acceptedUser.getName() : "A team member";
            String founderName = founder.getName() != null ? founder.getName() : "Founder";

            createNotification(founderId, "TEAM_MEMBER_ACCEPTED",
                    String.format("%s accepted your invitation to join startup #%d as %s", memberName, startupId, role));

            emailService.sendTeamMemberAcceptedEmail(founder.getEmail(), founderName, memberName, role, startupId);
        } catch (Exception e) {
            log.error("Error sending team member accepted notification: {}", e.getMessage());
        }
    }

    public void sendTeamMemberRejectedNotification(Long startupId, Long founderId, Long rejectedUserId, String role) {
        try {
            UserDTO founder = userServiceClient.getUserById(founderId);
            UserDTO rejectedUser = userServiceClient.getUserById(rejectedUserId);

            if (founder == null || founder.getEmail() == null) {
                log.warn("Founder {} has no email", founderId);
                return;
            }

            String memberName = rejectedUser != null ? rejectedUser.getName() : "A team member";
            String founderName = founder.getName() != null ? founder.getName() : "Founder";

            createNotification(founderId, "TEAM_MEMBER_REJECTED",
                    String.format("%s rejected your invitation to join startup #%d as %s", memberName, startupId, role));

            emailService.sendTeamMemberRejectedEmail(founder.getEmail(), founderName, memberName, role, startupId);
        } catch (Exception e) {
            log.error("Error sending team member rejected notification: {}", e.getMessage());
        }
    }

    public void sendPaymentCompletedNotification(Long investmentId, Long investorId, Long founderId) {
        try {
            UserDTO investor = userServiceClient.getUserById(investorId);
            UserDTO founder = userServiceClient.getUserById(founderId);

            if (investor != null && investor.getEmail() != null) {
                String investorName = investor.getName() != null ? investor.getName() : "Investor";
                createNotification(investorId, "PAYMENT_COMPLETED",
                        String.format("Your payment for investment #%d has been successfully processed", investmentId));
                emailService.sendPaymentCompletedEmail(investor.getEmail(), investorName, investmentId, "Amount processed");
            }

            if (founder != null && founder.getEmail() != null) {
                String founderName = founder.getName() != null ? founder.getName() : "Founder";
                createNotification(founderId, "PAYMENT_COMPLETED",
                        String.format("Payment completed for investment #%d in your startup", investmentId));
            }
        } catch (Exception e) {
            log.error("Error sending payment completed notification: {}", e.getMessage());
        }
    }

    public void sendPaymentFailedNotification(Long investmentId, Long investorId, String reason) {
        try {
            UserDTO investor = userServiceClient.getUserById(investorId);

            if (investor == null || investor.getEmail() == null) {
                log.warn("Investor {} has no email", investorId);
                return;
            }

            String investorName = investor.getName() != null ? investor.getName() : "Investor";
            createNotification(investorId, "PAYMENT_FAILED",
                    String.format("Payment failed for investment #%d. Reason: %s", investmentId, reason));

            emailService.sendPaymentFailedEmail(investor.getEmail(), investorName, investmentId, reason);
        } catch (Exception e) {
            log.error("Error sending payment failed notification: {}", e.getMessage());
        }
    }

    public void sendInvestmentApprovedNotification(Long investmentId, Long investorId, Long startupId, String amount) {
        try {
            UserDTO investor = userServiceClient.getUserById(investorId);

            if (investor == null || investor.getEmail() == null) {
                log.warn("Investor {} has no email", investorId);
                return;
            }

            String investorName = investor.getName() != null ? investor.getName() : "Investor";
            createNotification(investorId, "INVESTMENT_APPROVED",
                    String.format("Your investment #%d in startup #%d has been approved", investmentId, startupId));

            emailService.sendInvestmentApprovedEmail(investor.getEmail(), investorName, startupId, amount);
        } catch (Exception e) {
            log.error("Error sending investment approved notification: {}", e.getMessage());
        }
    }

    public void sendInvestmentRejectedNotification(Long investmentId, Long investorId, Long startupId, String amount, String reason) {
        try {
            UserDTO investor = userServiceClient.getUserById(investorId);

            if (investor == null || investor.getEmail() == null) {
                log.warn("Investor {} has no email", investorId);
                return;
            }

            String investorName = investor.getName() != null ? investor.getName() : "Investor";
            createNotification(investorId, "INVESTMENT_REJECTED",
                    String.format("Your investment #%d in startup #%d has been rejected", investmentId, startupId));

            emailService.sendInvestmentRejectedEmail(investor.getEmail(), investorName, startupId, amount, reason);
        } catch (Exception e) {
            log.error("Error sending investment rejected notification: {}", e.getMessage());
        }
    }
}
