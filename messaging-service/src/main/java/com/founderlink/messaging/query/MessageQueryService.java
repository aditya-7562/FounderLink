package com.founderlink.messaging.query;

import com.founderlink.messaging.dto.CursorPageDTO;
import com.founderlink.messaging.dto.MessageResponseDTO;
import com.founderlink.messaging.entity.Message;
import com.founderlink.messaging.exception.MessageNotFoundException;
import com.founderlink.messaging.repository.MessageRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
     * QUERY: Get full conversation between two users (non-paginated, cached).
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

    /** QUERY: Offset-paginated conversation (not cached; each Pageable is unique). */
    public Page<MessageResponseDTO> getConversation(Long user1, Long user2, Pageable pageable) {
        log.info("QUERY - getConversation (pageable): user1={}, user2={}, pageable={}", user1, user2, pageable);
        return messageRepository.findConversationPage(user1, user2, pageable)
                .map(this::mapToResponseDTO);
    }

    public List<MessageResponseDTO> getConversationFallback(Long user1, Long user2, Throwable throwable) {
        log.error("Circuit breaker fallback - getConversation. Reason: {}", throwable.getMessage());
        return Collections.emptyList();
    }

    /**
     * QUERY: Get all conversation partners for a user (non-paginated, cached).
     * Cache key = userId.
     */
    @CircuitBreaker(name = "messagingService", fallbackMethod = "getConversationPartnersFallback")
    @Retry(name = "messagingService")
    @Cacheable(value = "conversationPartners", key = "#userId")
    public List<Long> getConversationPartners(Long userId) {
        log.info("QUERY - getConversationPartners: userId={} (cache miss, hitting DB)", userId);
        return messageRepository.findConversationPartners(userId);
    }

    /** QUERY: Offset-paginated conversation partners. */
    public Page<Long> getConversationPartners(Long userId, Pageable pageable) {
        log.info("QUERY - getConversationPartners (pageable): userId={}, pageable={}", userId, pageable);
        return messageRepository.findConversationPartnersPage(userId, pageable);
    }

    public List<Long> getConversationPartnersFallback(Long userId, Throwable throwable) {
        log.error("Circuit breaker fallback - getConversationPartners. Reason: {}", throwable.getMessage());
        return Collections.emptyList();
    }

    /**
     * QUERY: Cursor-based conversation page.
     * <ul>
     *   <li>No cursors → initial load (most recent {@code size} messages)</li>
     *   <li>{@code beforeId} → load older history (scroll up)</li>
     *   <li>{@code afterId}  → catch-up / real-time gap fill after reconnect</li>
     * </ul>
     * Content is always returned in ascending id order (chronological).
     * Not cached: each cursor window is a unique snapshot; caching is
     * already handled by the existing non-paginated path for the initial view.
     */
    public CursorPageDTO<MessageResponseDTO> getConversationCursor(
            Long user1, Long user2, Long beforeId, Long afterId, int size) {

        log.info("QUERY - getConversationCursor: u1={}, u2={}, before={}, after={}, size={}",
                 user1, user2, beforeId, afterId, size);

        int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(0, safeSize);
        List<Message> raw;

        if (afterId != null) {
            // Catch-up: find messages newer than afterId, ASC — append to bottom
            raw = messageRepository.findAfter(user1, user2, afterId, pageable);
        } else {
            // Initial load OR load-older: findBefore returns DESC → reverse for ASC display
            long ceiling = (beforeId != null) ? beforeId : Long.MAX_VALUE;
            raw = messageRepository.findBefore(user1, user2, ceiling, pageable);
            Collections.reverse(raw);
        }

        List<MessageResponseDTO> content = raw.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());

        // nextCursor = oldest id in batch  → caller uses as ?before= for "load older"
        // prevCursor = newest id in batch  → caller uses as ?after=  for catch-up
        Long nextCursor = content.isEmpty() ? null : content.get(0).getId();
        Long prevCursor = content.isEmpty() ? null : content.get(content.size() - 1).getId();

        return CursorPageDTO.<MessageResponseDTO>builder()
                .content(content)
                .nextCursor(nextCursor)
                .prevCursor(prevCursor)
                .build();
    }

    // ── Shared mapper ─────────────────────────────────────────────────────────

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
