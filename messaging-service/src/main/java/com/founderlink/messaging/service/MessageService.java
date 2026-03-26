package com.founderlink.messaging.service;

import com.founderlink.messaging.command.MessageCommandService;
import com.founderlink.messaging.dto.MessageRequestDTO;
import com.founderlink.messaging.dto.MessageResponseDTO;
import com.founderlink.messaging.query.MessageQueryService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Facade that preserves the existing MessageService contract.
 * Delegates writes → MessageCommandService (CQRS Command side)
 * Delegates reads  → MessageQueryService   (CQRS Query side + Redis cache)
 */
@Service
public class MessageService {

    private final MessageCommandService commandService;
    private final MessageQueryService   queryService;

    public MessageService(MessageCommandService commandService, MessageQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    public MessageResponseDTO sendMessage(MessageRequestDTO requestDTO) {
        return commandService.sendMessage(requestDTO);
    }

    public MessageResponseDTO getMessageById(Long id) {
        return queryService.getMessageById(id);
    }

    public List<MessageResponseDTO> getConversation(Long user1, Long user2) {
        return queryService.getConversation(user1, user2);
    }

    public List<Long> getConversationPartners(Long userId) {
        return queryService.getConversationPartners(userId);
    }
}
