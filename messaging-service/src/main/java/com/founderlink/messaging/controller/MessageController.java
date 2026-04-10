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
    public ResponseEntity<MessageResponseDTO> sendMessage(@Valid @RequestBody MessageRequestDTO requestDTO) {
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
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Conversation fetched successfully")
    })
    public ResponseEntity<?> getConversation(
            @PathVariable Long user1,
            @PathVariable Long user2,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort,
            HttpServletRequest request) {
        log.info("GET /messages/conversation/{}/{} - getConversation", user1, user2);
        if (!isPaginatedRequest(request)) {
            return ResponseEntity.ok(messageService.getConversation(user1, user2));
        }

        Pageable pageable = buildPageable(page, size, sort, "createdAt", Sort.Direction.ASC);
        return ResponseEntity.ok(toPaginatedResponse(messageService.getConversation(user1, user2, pageable)));
    }

    @GetMapping("/partners/{userId}")
    @Operation(summary = "Get conversation partners", description = "Retrieves all user IDs that have had conversations with the given user.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Partners fetched successfully")
    })
    public ResponseEntity<?> getConversationPartners(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort,
            HttpServletRequest request) {
        log.info("GET /messages/partners/{} - getConversationPartners", userId);
        if (!isPaginatedRequest(request)) {
            return ResponseEntity.ok(messageService.getConversationPartners(userId));
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        return ResponseEntity.ok(toPaginatedResponse(messageService.getConversationPartners(userId, pageable)));
    }

    // ── Cursor-based endpoint (Step 3) ────────────────────────────────────────

    @GetMapping("/conversation/{user1}/{user2}/cursor")
    @Operation(
        summary = "Get conversation messages (cursor-based pagination)",
        description = "Stable, real-time-safe pagination using message IDs as cursors. " +
                      "Supply ?before=<id> to load older messages, ?after=<id> to catch up after a gap. " +
                      "Omit both to receive the most recent messages. " +
                      "nextCursor → older history (?before=), prevCursor → newer messages (?after=)."
    )
    public ResponseEntity<CursorPageDTO<MessageResponseDTO>> getConversationCursor(
            @PathVariable Long user1,
            @PathVariable Long user2,
            @RequestParam(required = false) Long before,
            @RequestParam(required = false) Long after,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /messages/conversation/{}/{}/cursor - before={}, after={}, size={}",
                 user1, user2, before, after, size);
                 
        if (before != null && after != null) {
            throw new com.founderlink.messaging.exception.InvalidMessageException("Cannot use both 'before' and 'after' cursors simultaneously");
        }
        
        CursorPageDTO<MessageResponseDTO> result =
                messageService.getConversationCursor(user1, user2, before, after, size);
        return ResponseEntity.ok(result);
    }

    private boolean isPaginatedRequest(HttpServletRequest request) {
        return request.getParameterMap().containsKey("page");
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
        String property = tokens[0].isBlank() ? defaultProperty : tokens[0].trim();
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
