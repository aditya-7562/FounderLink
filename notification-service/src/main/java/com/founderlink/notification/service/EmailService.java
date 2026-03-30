package com.founderlink.notification.service;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            message.setFrom("noreply@founderlink.com");

            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    public void sendBulkEmail(String[] recipients, String subject, String body) {
        for (String email : recipients) {
            sendEmail(email, subject, body);
        }
    }

    public void sendPasswordResetPinEmail(String to, String userName, String pin) {
        String subject = "Password Reset Request - FounderLink";
        String body = String.format(
                "Hello %s,\n\n" +
                "You have requested to reset your password.\n\n" +
                "Your 6-digit PIN is: %s\n\n" +
                "This PIN will expire in 5 minutes.\n\n" +
                "If you did not request this, please ignore this email.\n\n" +
                "Best regards,\n" +
                "FounderLink Team",
                userName, pin
        );
        sendEmail(to, subject, body);
    }

    public void sendTeamMemberAcceptedEmail(String founderEmail, String founderName, String memberName, String role, Long startupId) {
        String subject = "✅ Team Member Accepted Your Invitation";
        String body = String.format(
                "Hello %s,\n\n" +
                "Great news! %s has accepted your invitation to join your startup #%d as %s.\n\n" +
                "You can now collaborate with your new team member.\n\n" +
                "Best regards,\n" +
                "FounderLink Team",
                founderName, memberName, startupId, role
        );
        sendEmail(founderEmail, subject, body);
    }

    public void sendTeamMemberRejectedEmail(String founderEmail, String founderName, String memberName, String role, Long startupId) {
        String subject = "❌ Team Member Rejected Your Invitation";
        String body = String.format(
                "Hello %s,\n\n" +
                "%s has declined your invitation to join your startup #%d as %s.\n\n" +
                "You can send invitations to other candidates.\n\n" +
                "Best regards,\n" +
                "FounderLink Team",
                founderName, memberName, startupId, role
        );
        sendEmail(founderEmail, subject, body);
    }

    public void sendPaymentCompletedEmail(String email, String userName, Long investmentId, String amount) {
        String subject = "✅ Payment Successful - Investment Confirmed";
        String body = String.format(
                "Hello %s,\n\n" +
                "Your payment for investment #%d has been successfully processed.\n\n" +
                "Amount: %s\n\n" +
                "Your investment is now active. Thank you for your trust!\n\n" +
                "Best regards,\n" +
                "FounderLink Team",
                userName, investmentId, amount
        );
        sendEmail(email, subject, body);
    }

    public void sendPaymentFailedEmail(String email, String userName, Long investmentId, String reason) {
        String subject = "❌ Payment Failed - Action Required";
        String body = String.format(
                "Hello %s,\n\n" +
                "Unfortunately, your payment for investment #%d has failed.\n\n" +
                "Reason: %s\n\n" +
                "Please try again or contact support if the issue persists.\n\n" +
                "Best regards,\n" +
                "FounderLink Team",
                userName, investmentId, reason
        );
        sendEmail(email, subject, body);
    }

    public void sendInvestmentApprovedEmail(String investorEmail, String investorName, Long startupId, String amount) {
        String subject = "✅ Your Investment Was Approved";
        String body = String.format(
                "Hello %s,\n\n" +
                "Congratulations! The founder has approved your investment in startup #%d.\n\n" +
                "Investment Amount: %s\n\n" +
                "Payment processing will begin shortly.\n\n" +
                "Best regards,\n" +
                "FounderLink Team",
                investorName, startupId, amount
        );
        sendEmail(investorEmail, subject, body);
    }

    public void sendInvestmentRejectedEmail(String investorEmail, String investorName, Long startupId, String amount, String reason) {
        String subject = "❌ Your Investment Was Rejected";
        String body = String.format(
                "Hello %s,\n\n" +
                "We regret to inform you that your investment in startup #%d has been rejected by the founder.\n\n" +
                "Investment Amount: %s\n" +
                "Reason: %s\n\n" +
                "Your funds will be released back to your wallet shortly.\n\n" +
                "Best regards,\n" +
                "FounderLink Team",
                investorName, startupId, amount, reason != null ? reason : "Not specified"
        );
        sendEmail(investorEmail, subject, body);
    }
}
