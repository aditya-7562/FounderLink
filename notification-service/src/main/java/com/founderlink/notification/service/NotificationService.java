package com.founderlink.notification.service;

import com.founderlink.notification.client.StartupServiceClient;
import com.founderlink.notification.client.UserServiceClient;
import com.founderlink.notification.dto.NotificationResponseDTO;
import com.founderlink.notification.dto.StartupDTO;
import com.founderlink.notification.dto.UserDTO;
import com.founderlink.notification.entity.Notification;
import com.founderlink.notification.exception.NotificationNotFoundException;
import com.founderlink.notification.repository.NotificationRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserServiceClient userServiceClient;
    private final StartupServiceClient startupServiceClient;
    private final EmailService emailService;

    public NotificationService(NotificationRepository notificationRepository, 
                              UserServiceClient userServiceClient,
                              StartupServiceClient startupServiceClient,
                              EmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.userServiceClient = userServiceClient;
        this.startupServiceClient = startupServiceClient;
        this.emailService = emailService;
    }

    @CircuitBreaker(name = "notificationService", fallbackMethod = "createNotificationFallback")
    @Retry(name = "notificationService")
    public NotificationResponseDTO createNotification(Long userId, String type, String message) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setMessage(message);
        Notification saved = notificationRepository.save(notification);
        return mapToDTO(saved);
    }

    public NotificationResponseDTO createNotificationFallback(Long userId, String type, String message, Throwable throwable) {
        log.error("Fallback triggered for createNotification. User: {}, Type: {}, Reason: {}",
                userId, type, throwable.getMessage());
        return NotificationResponseDTO.builder()
                .userId(userId)
                .type(type)
                .message(message)
                .read(false)
                .build();
    }

    @CircuitBreaker(name = "notificationService", fallbackMethod = "getNotificationsByUserFallback")
    @Retry(name = "notificationService")
    public List<NotificationResponseDTO> getNotificationsByUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<NotificationResponseDTO> getNotificationsByUserFallback(Long userId, Throwable throwable) {
        log.error("Fallback triggered for getNotificationsByUser. User: {}, Reason: {}",
                userId, throwable.getMessage());
        return Collections.emptyList();
    }

    @CircuitBreaker(name = "notificationService", fallbackMethod = "getUnreadNotificationsFallback")
    @Retry(name = "notificationService")
    public List<NotificationResponseDTO> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<NotificationResponseDTO> getUnreadNotificationsFallback(Long userId, Throwable throwable) {
        log.error("Fallback triggered for getUnreadNotifications. User: {}, Reason: {}",
                userId, throwable.getMessage());
        return Collections.emptyList();
    }

    @CircuitBreaker(name = "notificationService", fallbackMethod = "markAsReadFallback")
    @Retry(name = "notificationService")
    public NotificationResponseDTO markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException(id));
        notification.setRead(true);
        Notification saved = notificationRepository.save(notification);
        return mapToDTO(saved);
    }

    public NotificationResponseDTO markAsReadFallback(Long id, Throwable throwable) {
        log.error("Fallback triggered for markAsRead. Notification ID: {}, Reason: {}",
                id, throwable.getMessage());
        if (throwable instanceof NotificationNotFoundException) {
            throw (NotificationNotFoundException) throwable;
        }
        return NotificationResponseDTO.builder()
                .id(id)
                .read(true)
                .build();
    }

    private NotificationResponseDTO mapToDTO(Notification notification) {
        return NotificationResponseDTO.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .type(notification.getType())
                .message(notification.getMessage())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    @CircuitBreaker(name = "notificationService", fallbackMethod = "notifyAllUsersFallback")
    @Retry(name = "notificationService")
    public void notifyAllUsers(String type, String message) {
        try {
            var users = userServiceClient.getAllUsers();
            log.info("Notifying {} users about event type: {}", users.size(), type);
            
            users.forEach(user -> {
                try {
                    createNotification(user.getId(), type, message);
                    log.info("Notification sent to user {}: {}", user.getId(), message);
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
        log.error("Fallback triggered for notifyAllUsers. Type: {}, Reason: {}", type, throwable.getMessage());
    }

    // ===== EMAIL NOTIFICATION METHODS =====

    public void sendStartupCreatedEmailToAllInvestors(Long startupId, String startupName, String industry, Double fundingGoal) {
        try {
            List<UserDTO> investors = userServiceClient.getUsersByRole("INVESTOR");
            log.info("Sending startup created email to {} investors", investors.size());

            String notificationMessage = String.format(
                    "New startup '%s' in %s is now open for investment. Funding goal: $%,.2f",
                    startupName, industry, fundingGoal
            );

            investors.forEach(user -> {
                try {
                    createNotification(user.getId(), "STARTUP_CREATED", notificationMessage);
                } catch (Exception e) {
                    log.error("Failed to create startup notification for user {}: {}", user.getId(), e.getMessage());
                }
            });

            String subject = "🚀 New Startup Alert: " + startupName;
            String body = String.format(
                    "Hi Investor,\n\n" +
                    "A new startup has been created that might interest you!\n\n" +
                    "Startup Name: %s\n" +
                    "Industry: %s\n" +
                    "Funding Goal: $%,.2f\n" +
                    "Startup ID: %d\n\n" +
                    "Check it out and consider investing!\n\n" +
                    "Best regards,\n" +
                    "FounderLink Team",
                    startupName, industry, fundingGoal, startupId
            );

            String[] emails = investors.stream()
                    .map(UserDTO::getEmail)
                    .filter(email -> email != null && !email.isEmpty())
                    .toArray(String[]::new);

            emailService.sendBulkEmail(emails, subject, body);
            log.info("Startup created emails sent to {} investors", emails.length);
        } catch (Exception e) {
            log.error("Error sending startup created emails: {}", e.getMessage());
        }
    }

    public void sendInvestmentInterestEmailToFounder(Long startupId, Long investorId, String investorName) {
        try {
            StartupDTO startup = startupServiceClient.getStartupById(startupId);
            if (startup == null || startup.getFounderId() == null) {
                log.warn("Startup {} has no founder information, cannot send notification", startupId);
                return;
            }

            UserDTO founder = userServiceClient.getUserById(startup.getFounderId());
            UserDTO investor = userServiceClient.getUserById(investorId);
            if (founder == null || founder.getEmail() == null) {
                log.warn("Founder {} has no email, cannot send notification", startup.getFounderId());
                return;
            }

            String resolvedInvestorName = investorName;
            if ((resolvedInvestorName == null || resolvedInvestorName.isBlank()) && investor != null) {
                resolvedInvestorName = investor.getName();
            }
            if (resolvedInvestorName == null || resolvedInvestorName.isBlank()) {
                resolvedInvestorName = "An investor";
            }

            String investorEmail = investor != null ? investor.getEmail() : "Not available";
            String notificationMessage = String.format(
                    "%s is interested in your startup #%d",
                    resolvedInvestorName,
                    startupId
            );

            createNotification(startup.getFounderId(), "INVESTMENT_INTERESTED", notificationMessage);

            String subject = "💡 Investor Interest in Your Startup #" + startupId;
            String body = String.format(
                    "Hi Founder,\n\n" +
                    "Great news! An investor is interested in your startup!\n\n" +
                    "Investor Name: %s\n" +
                    "Investor Email: %s\n" +
                    "Startup ID: %d\n\n" +
                    "This investor might be interested in investing. Reach out to discuss further!\n\n" +
                    "Best regards,\n" +
                    "FounderLink Team",
                    resolvedInvestorName, investorEmail, startupId
            );

            emailService.sendEmail(founder.getEmail(), subject, body);
            log.info("Investment interest email sent to founder {} for startup {}", startup.getFounderId(), startupId);
        } catch (Exception e) {
            log.error("Error sending investment interest email: {}", e.getMessage());
        }
    }

    public void sendInvestmentCreatedEmailToFounder(Long startupId, Long founderId, Long investorId, Double amount) {
        try {
            UserDTO founder = userServiceClient.getUserById(founderId);
            UserDTO investor = userServiceClient.getUserById(investorId);

            if (founder == null || founder.getEmail() == null) {
                log.warn("Founder {} has no email, cannot send notification", founderId);
                return;
            }

            String investorName = investor != null ? investor.getName() : "Unknown Investor";
                String notificationMessage = String.format(
                    "%s showed investment interest in startup #%d with $%,.2f",
                    investorName, startupId, amount
                );

                createNotification(founderId, "INVESTMENT_CREATED", notificationMessage);

                String subject = "💡 New Investor Interest in Startup #" + startupId;
            String body = String.format(
                    "Hi Founder,\n\n" +
                    "An investor has shown interest in your startup.\n\n" +
                    "Investor Name: %s\n" +
                    "Interested Amount: $%,.2f\n" +
                    "Startup ID: %d\n\n" +
                    "Review the investment request and connect with the investor if you want to proceed.\n\n" +
                    "Best regards,\n" +
                    "FounderLink Team",
                    investorName, amount, startupId
            );

            emailService.sendEmail(founder.getEmail(), subject, body);
            log.info("Investment created email sent to founder {} for startup {}", founderId, startupId);
        } catch (Exception e) {
            log.error("Error sending investment created email: {}", e.getMessage());
        }
    }

    public void sendTeamInviteEmail(Long startupId, Long invitedUserId, String role) {
        try {
            UserDTO invitedUser = userServiceClient.getUserById(invitedUserId);
            if (invitedUser == null || invitedUser.getEmail() == null) {
                log.warn("Invited user {} has no email, cannot send team invite", invitedUserId);
                return;
            }

            String subject = "🤝 Team Invitation for Startup #" + startupId;
            String body = String.format(
                    "Hi %s,\n\n" +
                    "You have been invited to join startup #%d as %s.\n\n" +
                    "Please log in to FounderLink to review the invitation.\n\n" +
                    "Best regards,\n" +
                    "FounderLink Team",
                    invitedUser.getName() != null ? invitedUser.getName() : "User",
                    startupId,
                    role
            );

            emailService.sendEmail(invitedUser.getEmail(), subject, body);
            log.info("Team invite email sent to user {} for startup {}", invitedUserId, startupId);
        } catch (Exception e) {
            log.error("Error sending team invite email: {}", e.getMessage());
        }
    }
}
