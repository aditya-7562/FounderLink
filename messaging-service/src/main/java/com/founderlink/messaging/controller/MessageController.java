package com.founderlink.messaging.controller;

import com.founderlink.messaging.dto.MessageRequestDTO;
import com.founderlink.messaging.dto.MessageResponseDTO;
import com.founderlink.messaging.service.MessageService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Message sent successfully")
    })
    public ResponseEntity<MessageResponseDTO> sendMessage(@Valid @RequestBody MessageRequestDTO requestDTO) {
        log.info("POST /messages - sendMessage from: {} to: {}", requestDTO.getSenderId(), requestDTO.getReceiverId());
        MessageResponseDTO response = messageService.sendMessage(requestDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get message by ID", description = "Retrieves a message by its unique ID.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Message fetched successfully")
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
    public ResponseEntity<List<MessageResponseDTO>> getConversation(
            @PathVariable Long user1, @PathVariable Long user2) {
        log.info("GET /messages/conversation/{}/{} - getConversation", user1, user2);
        return ResponseEntity.ok(messageService.getConversation(user1, user2));
    }

    @GetMapping("/partners/{userId}")
    @Operation(summary = "Get conversation partners", description = "Retrieves all user IDs that have had conversations with the given user.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Partners fetched successfully")
    })
    public ResponseEntity<List<Long>> getConversationPartners(@PathVariable Long userId) {
        log.info("GET /messages/partners/{} - getConversationPartners", userId);
        return ResponseEntity.ok(messageService.getConversationPartners(userId));
    }
}
