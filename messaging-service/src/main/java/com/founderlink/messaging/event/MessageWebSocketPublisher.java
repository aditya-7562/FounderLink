package com.founderlink.messaging.event;

import com.founderlink.messaging.dto.MessageResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Pushes persisted messages to all connected STOMP subscribers on the conversation topic.
 *
 * <p>Topic key is normalized: {@code /topic/conversation/{lo}/{hi}} where
 * {@code lo = min(user1, user2)} and {@code hi = max(user1, user2)}.
 * This guarantees both participants share one stable topic regardless of which user
 * initiated the conversation.
 *
 * <p>Push failures are intentionally caught and logged rather than propagated —
 * the REST transaction must not roll back because a STOMP push failed.
 * The frontend handles gaps via the {@code ?after=} catch-up cursor endpoint.
 */
@Component
public class MessageWebSocketPublisher {

    private static final Logger log = LoggerFactory.getLogger(MessageWebSocketPublisher.class);

    private final SimpMessagingTemplate messagingTemplate;

    public MessageWebSocketPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Push a message DTO to all clients subscribed to {@code /topic/conversation/{lo}/{hi}}.
     *
     * @param senderId   sender user ID
     * @param receiverId receiver user ID
     * @param message    the persisted message to broadcast
     */
    public void pushToConversation(Long senderId, Long receiverId, MessageResponseDTO message) {
        String topic = conversationTopic(senderId, receiverId);
        try {
            messagingTemplate.convertAndSend(topic, message);
            log.info("WS PUSH → {}: messageId={}", topic, message.getId());
        } catch (Exception ex) {
            // Non-fatal: client will catch up via ?after= cursor endpoint on reconnect
            log.warn("WS PUSH failed for topic={}, messageId={}: {}", topic, message.getId(), ex.getMessage());
        }
    }

    /**
     * Returns the canonical STOMP topic for the conversation between two users.
     * The smaller ID is always the first component to avoid duplicate topics.
     */
    public static String conversationTopic(Long a, Long b) {
        long lo = Math.min(a, b);
        long hi = Math.max(a, b);
        return "/topic/conversation/" + lo + "/" + hi;
    }
}
