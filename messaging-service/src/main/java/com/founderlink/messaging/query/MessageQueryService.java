package com.founderlink.messaging.query;

import com.founderlink.messaging.dto.MessageResponseDTO;
import com.founderlink.messaging.entity.Message;
import com.founderlink.messaging.exception.MessageNotFoundException;
import com.founderlink.messaging.repository.MessageRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageQueryService {

    private static final Logger log = LoggerFactory.getLogger(MessageQueryService.class);

    private final MessageRepository messageRepository;

    public MessageQueryService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    /**
     * QUERY: Get a single message by ID.
     * Cache key = messageId.
     */
    @Cacheable(value = "messageById", key = "#id")
    public MessageResponseDTO getMessageById(Long id) {
        log.info("QUERY - getMessageById: id={} (cache miss, hitting DB)", id);
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> new MessageNotFoundException(id));
        return mapToResponseDTO(message);
    }

    /**
     * QUERY: Get full conversation between two users.
     * Cache key = sorted user pair to avoid duplicate entries for (1,2) vs (2,1).
     */
    @CircuitBreaker(name = "messagingService", fallbackMethod = "getConversationFallback")
    @Retry(name = "messagingService")
    @Cacheable(value = "conversation",
               key = "(#user1 < #user2 ? #user1 + '_' + #user2 : #user2 + '_' + #user1)")
    public List<MessageResponseDTO> getConversation(Long user1, Long user2) {
        log.info("QUERY - getConversation: user1={}, user2={} (cache miss, hitting DB)", user1, user2);
        return messageRepository.findConversation(user1, user2).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<MessageResponseDTO> getConversationFallback(Long user1, Long user2, Throwable throwable) {
        log.error("Circuit breaker fallback - getConversation. Reason: {}", throwable.getMessage());
        return Collections.emptyList();
    }

    /**
     * QUERY: Get all conversation partners for a user.
     * Cache key = userId.
     */
    @CircuitBreaker(name = "messagingService", fallbackMethod = "getConversationPartnersFallback")
    @Retry(name = "messagingService")
    @Cacheable(value = "conversationPartners", key = "#userId")
    public List<Long> getConversationPartners(Long userId) {
        log.info("QUERY - getConversationPartners: userId={} (cache miss, hitting DB)", userId);
        return messageRepository.findConversationPartners(userId);
    }

    public List<Long> getConversationPartnersFallback(Long userId, Throwable throwable) {
        log.error("Circuit breaker fallback - getConversationPartners. Reason: {}", throwable.getMessage());
        return Collections.emptyList();
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
