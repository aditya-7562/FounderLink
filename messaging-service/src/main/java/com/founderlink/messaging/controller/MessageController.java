package com.founderlink.messaging.controller;

import com.founderlink.messaging.dto.CursorPageDTO;
import com.founderlink.messaging.dto.MessageRequestDTO;
import com.founderlink.messaging.dto.MessageResponseDTO;
import com.founderlink.messaging.service.MessageService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@Slf4j
@RestController
@RequestMapping("/messages")
@Tag(name = "Messages", description = "Endpoints for sending and retrieving messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    @Operation(summary = "Send a message", description = "Sends a new message from one user to another.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Message sent successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed — invalid request body"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "User service unavailable")
    })
    public ResponseEntity<MessageResponseDTO> sendMessage(
            @RequestHeader("X-User-Id") Long authenticatedUserId,
            @Valid @RequestBody MessageRequestDTO requestDTO) {
        // Override sender with authenticated user - never trust client-provided senderId
        requestDTO.setSenderId(authenticatedUserId);
        log.info("POST /messages - sendMessage from: {} to: {}", requestDTO.getSenderId(), requestDTO.getReceiverId());
        MessageResponseDTO response = messageService.sendMessage(requestDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id:\\d+}")
    @Operation(summary = "Get message by ID", description = "Retrieves a message by its unique ID.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Message fetched successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Message not found")
    })
    public ResponseEntity<MessageResponseDTO> getMessageById(@PathVariable Long id) {
        log.info("GET /messages/{} - getMessageById", id);
        return ResponseEntity.ok(messageService.getMessageById(id));
    }

    @GetMapping("/conversation/{user1}/{user2}")
    @Operation(summary = "Get conversation between users", description = "Retrieves all messages exchanged between two users.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Conversation fetched successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — user not part of conversation"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "Service unavailable")
    })
    public ResponseEntity<?> getConversation(
            @PathVariable Long user1,
            @PathVariable Long user2,
            @RequestHeader(value = "X-User-Id", required = false) Long authenticatedUserId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestParam Map<String, String> params) {

        // RBAC: Only participants OR Admin can view conversation
        if (authenticatedUserId != null && !authenticatedUserId.equals(user1) && !authenticatedUserId.equals(user2)
                && !"ROLE_ADMIN".equals(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (isPaginatedRequest(params)) {
            int page = Integer.parseInt(params.getOrDefault("page", "0"));
            int size = Integer.parseInt(params.getOrDefault("size", "20"));
            Pageable pageable = buildPageable(page, size, params.get("sort"), "createdAt", Sort.Direction.DESC);
            return ResponseEntity.ok(toPaginatedResponse(messageService.getConversation(user1, user2, pageable)));
        }

        return ResponseEntity.ok(messageService.getConversation(user1, user2));
    }

    @GetMapping("/partners/{userId}")
    @Operation(summary = "Get conversation partners", description = "Retrieves IDs of all users the specified user has exchanged messages with.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Partners fetched successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — user not allowed to view partners")
    })
    public ResponseEntity<?> getConversationPartners(
            @PathVariable Long userId,
            @RequestHeader(value = "X-User-Id", required = false) Long authenticatedUserId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestParam Map<String, String> params) {

        if (authenticatedUserId != null && !authenticatedUserId.equals(userId) && !"ROLE_ADMIN".equals(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (isPaginatedRequest(params)) {
            int page = Integer.parseInt(params.getOrDefault("page", "0"));
            int size = Integer.parseInt(params.getOrDefault("size", "20"));
            // Sort handled by JPQL (ORDER BY MAX(m.createdAt) DESC) — unsorted Pageable avoids
            // Spring Data appending a conflicting ORDER BY that violates ONLY_FULL_GROUP_BY
            Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
            return ResponseEntity.ok(toPaginatedResponse(messageService.getConversationPartners(userId, pageable)));
        }

        return ResponseEntity.ok(messageService.getConversationPartners(userId));
    }

    @GetMapping("/conversation/{user1}/{user2}/cursor")
    @Operation(summary = "Get conversation by cursor", description = "Retrieves messages using cursor-based pagination for real-time sync.")
    public ResponseEntity<CursorPageDTO<MessageResponseDTO>> getConversationCursor(
            @PathVariable Long user1,
            @PathVariable Long user2,
            @RequestHeader(value = "X-User-Id", required = false) Long authenticatedUserId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestParam(required = false) Long before,
            @RequestParam(required = false) Long after,
            @RequestParam(defaultValue = "20") int size) {

        if (authenticatedUserId != null && !authenticatedUserId.equals(user1) && !authenticatedUserId.equals(user2)
                && !"ROLE_ADMIN".equals(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        log.info("GET /messages/conversation/{}/{}/cursor - before={}, after={}, size={}",
                 user1, user2, before, after, size);
                 
        if (before != null && after != null) {
            throw new com.founderlink.messaging.exception.InvalidMessageException("Cannot use both 'before' and 'after' cursors simultaneously");
        }
        
        CursorPageDTO<MessageResponseDTO> result =
                messageService.getConversationCursor(user1, user2, before, after, size);
        return ResponseEntity.ok(result);
    }

    private boolean isPaginatedRequest(Map<String, String> params) {
        return params.containsKey("page");
    }

    private Pageable buildPageable(int page, int size, String sort, String defaultProperty, Sort.Direction defaultDirection) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Sort resolvedSort = resolveSort(sort, defaultProperty, defaultDirection);
        return PageRequest.of(safePage, safeSize, resolvedSort);
    }

    private Sort resolveSort(String sort, String defaultProperty, Sort.Direction defaultDirection) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(defaultDirection, defaultProperty);
        }

        String[] tokens = sort.split(",");
        String property = (tokens.length > 0 && !tokens[0].isBlank()) ? tokens[0].trim() : defaultProperty;
        Sort.Direction direction = tokens.length > 1 && "desc".equalsIgnoreCase(tokens[1].trim())
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direction, property);
    }

    private <T> Map<String, Object> toPaginatedResponse(Page<T> page) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("content", page.getContent());
        payload.put("page", page.getNumber());
        payload.put("size", page.getSize());
        payload.put("totalElements", page.getTotalElements());
        payload.put("totalPages", page.getTotalPages());
        payload.put("last", page.isLast());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", payload);
        response.put("error", null);
        return response;
    }
}
