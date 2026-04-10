package com.founderlink.messaging.command;

import com.founderlink.messaging.client.UserServiceClient;
import com.founderlink.messaging.dto.MessageRequestDTO;
import com.founderlink.messaging.dto.MessageResponseDTO;
import com.founderlink.messaging.dto.UserDTO;
import com.founderlink.messaging.entity.Message;
import com.founderlink.messaging.event.MessageEventPublisher;
import com.founderlink.messaging.event.MessageWebSocketPublisher;
import com.founderlink.messaging.exception.InvalidMessageException;
import com.founderlink.messaging.repository.MessageRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

@Service
public class MessageCommandService {

    private static final Logger log = LoggerFactory.getLogger(MessageCommandService.class);

    private final MessageRepository           messageRepository;
    private final UserServiceClient           userServiceClient;
    private final MessageEventPublisher       messageEventPublisher;
    private final MessageWebSocketPublisher   wsPublisher;

    public MessageCommandService(MessageRepository messageRepository,
                                  UserServiceClient userServiceClient,
                                  MessageEventPublisher messageEventPublisher,
                                  MessageWebSocketPublisher wsPublisher) {
        this.messageRepository     = messageRepository;
        this.userServiceClient     = userServiceClient;
        this.messageEventPublisher = messageEventPublisher;
        this.wsPublisher           = wsPublisher;
    }

    /**
     * COMMAND: Send a message.
     *
     * <ol>
     *   <li>Validate sender ≠ receiver and both users exist (via Feign + circuit breaker)</li>
     *   <li>Persist to DB ({@code saveAndFlush} — synchronous, source of truth)</li>
     *   <li>Push via STOMP WebSocket to both conversation participants (non-fatal on failure)</li>
     *   <li>Publish {@code message.sent} RabbitMQ event for notification-service (non-fatal)</li>
     *   <li>Evict the relevant Redis caches</li>
     * </ol>
     */
    @CircuitBreaker(name = "messagingService", fallbackMethod = "sendMessageFallback")
    @Retry(name = "messagingService")
    @Caching(evict = {
        @CacheEvict(value = "conversation",         allEntries = true),
        @CacheEvict(value = "conversationPartners", key = "#requestDTO.senderId"),
        @CacheEvict(value = "conversationPartners", key = "#requestDTO.receiverId")
    })
    public MessageResponseDTO sendMessage(MessageRequestDTO requestDTO) {
        log.info("COMMAND - sendMessage: senderId={}, receiverId={}",
                 requestDTO.getSenderId(), requestDTO.getReceiverId());

        if (requestDTO.getSenderId().equals(requestDTO.getReceiverId())) {
            throw new InvalidMessageException("Sender and receiver cannot be the same user");
        }

        UserDTO sender = userServiceClient.getUserById(requestDTO.getSenderId());
        if (sender == null) {
            throw new InvalidMessageException("Sender with ID " + requestDTO.getSenderId() + " does not exist");
        }

        UserDTO receiver = userServiceClient.getUserById(requestDTO.getReceiverId());
        if (receiver == null) {
            throw new InvalidMessageException("Receiver with ID " + requestDTO.getReceiverId() + " does not exist");
        }

        Message message = new Message();
        message.setSenderId(requestDTO.getSenderId());
        message.setReceiverId(requestDTO.getReceiverId());
        message.setContent(requestDTO.getContent());

        Message saved = messageRepository.saveAndFlush(message);
        MessageResponseDTO responseDTO = mapToResponseDTO(saved);

        // ── Step 4: STOMP push (non-fatal) ───────────────────────────────────
        // Both sender and receiver share the same normalized topic.
        // Frontend deduplicates by message id, so sender's own write is idempotent.
        wsPublisher.pushToConversation(saved.getSenderId(), saved.getReceiverId(), responseDTO);

        // ── RabbitMQ event for notification-service (unchanged) ───────────────
        try {
            String senderName = sender.getName() != null ? sender.getName() : "Someone";
            messageEventPublisher.publishMessageSent(
                    saved.getId(), saved.getSenderId(), saved.getReceiverId(), senderName);
        } catch (Exception e) {
            log.warn("Failed to publish message event: {}", e.getMessage());
        }

        return responseDTO;
    }

    public MessageResponseDTO sendMessageFallback(MessageRequestDTO requestDTO, Throwable throwable) {
        log.error("CIRCUIT BREAKER FALLBACK - sendMessage. Reason: {}", throwable.getMessage());
        throw new InvalidMessageException("Cannot send message: User Service is unavailable.");
    }

    private MessageResponseDTO mapToResponseDTO(Message message) {
        return MessageResponseDTO.builder()
                .id(message.getId())
                .senderId(message.getSenderId())
                .receiverId(message.getReceiverId())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
