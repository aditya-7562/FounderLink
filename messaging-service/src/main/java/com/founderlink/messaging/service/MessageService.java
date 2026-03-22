package com.founderlink.messaging.service;

import com.founderlink.messaging.client.UserServiceClient;
import com.founderlink.messaging.dto.MessageRequestDTO;
import com.founderlink.messaging.dto.MessageResponseDTO;
import com.founderlink.messaging.dto.UserDTO;
import com.founderlink.messaging.entity.Message;
import com.founderlink.messaging.event.MessageEventPublisher;
import com.founderlink.messaging.exception.InvalidMessageException;
import com.founderlink.messaging.exception.MessageNotFoundException;
import com.founderlink.messaging.repository.MessageRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageService.class);

    private final MessageRepository messageRepository;
    private final UserServiceClient userServiceClient;
    private final MessageEventPublisher messageEventPublisher;

    public MessageService(MessageRepository messageRepository, UserServiceClient userServiceClient,
                          MessageEventPublisher messageEventPublisher) {
        this.messageRepository = messageRepository;
        this.userServiceClient = userServiceClient;
        this.messageEventPublisher = messageEventPublisher;
    }

    @CircuitBreaker(name = "messagingService", fallbackMethod = "sendMessageFallback")
    @Retry(name = "messagingService")
    public MessageResponseDTO sendMessage(MessageRequestDTO requestDTO) {
        log.info("======= STARTING MESSAGE VALIDATION =======");
        log.info("Validating message from sender ID: {} to receiver ID: {}", 
                requestDTO.getSenderId(), requestDTO.getReceiverId());
        
        if (requestDTO.getSenderId().equals(requestDTO.getReceiverId())) {
            log.error("✗ VALIDATION FAILED: Sender and receiver cannot be the same user");
            throw new InvalidMessageException("Sender and receiver cannot be the same user");
        }

        // Validate sender exists via user-service (Feign)
        log.info("Step 1: Calling user-service to validate SENDER (ID: {})", requestDTO.getSenderId());
        UserDTO sender = null;
        try {
            sender = userServiceClient.getUserById(requestDTO.getSenderId());
            if (sender != null) {
                log.info("✓ SENDER VALIDATION SUCCESS: User {} found - Name: {}, Email: {}", 
                        sender.getId(), sender.getName(), sender.getEmail());
            } else {
                log.error("✗ SENDER VALIDATION FAILED: Could not validate sender ID: {} - user does not exist",
                        requestDTO.getSenderId());
                throw new InvalidMessageException("Sender with ID " + requestDTO.getSenderId() + " does not exist");
            }
        } catch (Exception e) {
            log.error("✗ SENDER VALIDATION ERROR: Exception calling user-service for sender {}: {}", 
                    requestDTO.getSenderId(), e.getMessage());
            throw new InvalidMessageException("Failed to validate sender: " + e.getMessage());
        }

        // Validate receiver exists via user-service (Feign)
        log.info("Step 2: Calling user-service to validate RECEIVER (ID: {})", requestDTO.getReceiverId());
        UserDTO receiver = null;
        try {
            receiver = userServiceClient.getUserById(requestDTO.getReceiverId());
            if (receiver != null) {
                log.info("✓ RECEIVER VALIDATION SUCCESS: User {} found - Name: {}, Email: {}", 
                        receiver.getId(), receiver.getName(), receiver.getEmail());
            } else {
                log.error("✗ RECEIVER VALIDATION FAILED: Could not validate receiver ID: {} - user does not exist",
                        requestDTO.getReceiverId());
                throw new InvalidMessageException("Receiver with ID " + requestDTO.getReceiverId() + " does not exist");
            }
        } catch (Exception e) {
            log.error("✗ RECEIVER VALIDATION ERROR: Exception calling user-service for receiver {}: {}", 
                    requestDTO.getReceiverId(), e.getMessage());
            throw new InvalidMessageException("Failed to validate receiver: " + e.getMessage());
        }

        log.info("Step 3: Both users validated successfully. Creating message...");
        Message message = new Message();
        message.setSenderId(requestDTO.getSenderId());
        message.setReceiverId(requestDTO.getReceiverId());
        message.setContent(requestDTO.getContent());

        Message saved = messageRepository.save(message);
        log.info("✓ MESSAGE SAVED SUCCESSFULLY - Message ID: {}", saved.getId());

        // Publish event to notify the receiver
        try {
            String senderName = sender.getName() != null ? sender.getName() : "Someone";
            messageEventPublisher.publishMessageSent(saved.getId(), saved.getSenderId(),
                    saved.getReceiverId(), senderName);
        } catch (Exception e) {
            log.warn("Failed to publish message event (notification may not be sent): {}", e.getMessage());
        }

        log.info("======= MESSAGE VALIDATION COMPLETE =======");
        
        return mapToResponseDTO(saved);
    }

    public MessageResponseDTO sendMessageFallback(MessageRequestDTO requestDTO, Throwable throwable) {
        log.error("✗ CIRCUIT BREAKER FALLBACK: Cannot save message - User Service is unavailable and strict validation is enabled. Reason: {}", 
                throwable.getMessage());
        throw new InvalidMessageException("Cannot send message: User Service is unavailable. Validation is required before saving messages.");
    }

    @CircuitBreaker(name = "messagingService", fallbackMethod = "getConversationFallback")
    @Retry(name = "messagingService")
    public List<MessageResponseDTO> getConversation(Long user1, Long user2) {
        return messageRepository.findConversation(user1, user2).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<MessageResponseDTO> getConversationFallback(Long user1, Long user2, Throwable throwable) {
        log.error("Circuit breaker fallback triggered for getConversation. Reason: {}", throwable.getMessage());
        return Collections.emptyList();
    }

    @CircuitBreaker(name = "messagingService", fallbackMethod = "getConversationPartnersFallback")
    @Retry(name = "messagingService")
    public List<Long> getConversationPartners(Long userId) {
        return messageRepository.findConversationPartners(userId);
    }

    public List<Long> getConversationPartnersFallback(Long userId, Throwable throwable) {
        log.error("Circuit breaker fallback triggered for getConversationPartners. Reason: {}", throwable.getMessage());
        return Collections.emptyList();
    }

    public MessageResponseDTO getMessageById(Long id) {
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> new MessageNotFoundException(id));
        return mapToResponseDTO(message);
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
