package com.founderlink.payment.dlq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Dead Letter Queue (DLQ) handler for failed saga events.
 * 
 * Receives events that failed processing after retries are exhausted.
 * Provides logging, monitoring, and potential manual intervention hooks.
 * 
 * Failure scenarios:
 * 1. PaymentGateway unavailable - payment hold/capture/release fails
 * 2. WalletService unavailable - deposit fails after successful capture
 * 3. Database errors - payment record cannot be saved
 * 4. Configuration errors - invalid routing or queue setup
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DeadLetterQueueHandler {

    private final ObjectMapper objectMapper;
    private final DeadLetterLogRepository dlqLogRepository;

    // Counters for monitoring
    private volatile long dlqMessageCount = 0;
    private volatile long dlqProcessingErrors = 0;

    /**
     * Listen to Dead Letter Queue.
     * This method is called when a message fails after all retries.
     */
    @RabbitListener(queues = "founderlink.dlq")
    public void handleDeadLetterMessage(String message) {
        String dlqId = UUID.randomUUID().toString();
        LocalDateTime receivedAt = LocalDateTime.now();
        dlqMessageCount++;

        log.error("═══════════════════════════════════════════════════════════");
        log.error("⚠️  DEAD LETTER QUEUE MESSAGE RECEIVED [{}]", dlqId);
        log.error("═══════════════════════════════════════════════════════════");
        log.error("Message: {}", message);
        log.error("Received at: {}", receivedAt);

        try {
            // Parse message to extract metadata
            JsonNode messageJson = objectMapper.readTree(message);
            
            String eventType = extractEventType(messageJson);
            String investmentId = extractInvestmentId(messageJson);
            String investorId = extractInvestorId(messageJson);

            log.error("Event Type: {}", eventType);
            log.error("Investment ID: {}", investmentId);
            log.error("Investor ID: {}", investorId);
            log.error("DLQ Entry ID: {}", dlqId);

            // Store in database for audit trail and manual review
            DeadLetterLog dlqLog = new DeadLetterLog();
            dlqLog.setDlqId(dlqId);
            dlqLog.setEventType(eventType);
            dlqLog.setInvestmentId(investmentId);
            dlqLog.setInvestorId(investorId);
            dlqLog.setMessagePayload(message);
            dlqLog.setReceivedAt(receivedAt);
            dlqLog.setStatus("RECEIVED");

            dlqLogRepository.save(dlqLog);

            // TODO: Send alert to operations team
            // - Email notification to payments-ops@founderlink.com
            // - Slack notification to #payment-alerts
            // - PagerDuty alert for critical failures

            log.error("✓ DLQ message logged for manual review. ID: {}", dlqId);
            log.error("═══════════════════════════════════════════════════════════");

        } catch (Exception e) {
            dlqProcessingErrors++;
            log.error("✗ Error processing DLQ message: {}", e.getMessage(), e);
            log.error("Please investigate manually - this failure may indicate system issues");

            // Even if DLQ handler fails, we should persist the raw message somehow
            // This prevents message loss in catastrophic failure scenarios
        }
    }

    /**
     * Extract event type from message JSON.
     * Used for categorizing DLQ messages.
     */
    private String extractEventType(JsonNode messageJson) {
        if (messageJson.has("eventType")) {
            return messageJson.get("eventType").asText("UNKNOWN");
        }
        
        // Try to infer from message structure
        if (messageJson.has("investmentId") && messageJson.has("rejectionReason")) {
            return "InvestmentRejectedEvent";
        } else if (messageJson.has("investmentId") && messageJson.has("amount")) {
            return "InvestmentCreatedEvent";
        }
        
        return "UNKNOWN";
    }

    private String extractInvestmentId(JsonNode messageJson) {
        return messageJson.has("investmentId") 
            ? messageJson.get("investmentId").asText("N/A") 
            : "N/A";
    }

    private String extractInvestorId(JsonNode messageJson) {
        return messageJson.has("investorId") 
            ? messageJson.get("investorId").asText("N/A") 
            : "N/A";
    }

    /**
     * Get current DLQ statistics.
     * Used for monitoring dashboard.
     */
    public DeadLetterQueueStats getStats() {
        return new DeadLetterQueueStats(
            dlqMessageCount,
            dlqProcessingErrors,
            dlqMessageCount - dlqProcessingErrors
        );
    }

    /**
     * Statistics about DLQ messages.
     */
    public record DeadLetterQueueStats(
        long totalMessagesReceived,
        long processingErrors,
        long successfullyLogged
    ) {}
}
