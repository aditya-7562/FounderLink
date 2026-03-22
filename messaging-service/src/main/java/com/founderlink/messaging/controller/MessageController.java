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

@Slf4j
@RestController
@RequestMapping("/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    public ResponseEntity<MessageResponseDTO> sendMessage(@Valid @RequestBody MessageRequestDTO requestDTO) {
        log.info("POST /messages - sendMessage from: {} to: {}", requestDTO.getSenderId(), requestDTO.getReceiverId());
        MessageResponseDTO response = messageService.sendMessage(requestDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MessageResponseDTO> getMessageById(@PathVariable Long id) {
        log.info("GET /messages/{} - getMessageById", id);
        return ResponseEntity.ok(messageService.getMessageById(id));
    }

    @GetMapping("/conversation/{user1}/{user2}")
    public ResponseEntity<List<MessageResponseDTO>> getConversation(
            @PathVariable Long user1, @PathVariable Long user2) {
        log.info("GET /messages/conversation/{}/{} - getConversation", user1, user2);
        return ResponseEntity.ok(messageService.getConversation(user1, user2));
    }

    @GetMapping("/partners/{userId}")
    public ResponseEntity<List<Long>> getConversationPartners(@PathVariable Long userId) {
        log.info("GET /messages/partners/{} - getConversationPartners", userId);
        return ResponseEntity.ok(messageService.getConversationPartners(userId));
    }
}
