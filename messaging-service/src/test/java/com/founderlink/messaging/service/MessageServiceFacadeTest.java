package com.founderlink.messaging.service;

import com.founderlink.messaging.command.MessageCommandService;
import com.founderlink.messaging.dto.MessageRequestDTO;
import com.founderlink.messaging.dto.MessageResponseDTO;
import com.founderlink.messaging.query.MessageQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MessageServiceFacadeTest {

    @Mock
    private MessageCommandService commandService;

    @Mock
    private MessageQueryService queryService;

    @InjectMocks
    private MessageService messageService;

    @Test
    @DisplayName("sendMessage - delegates to commandService")
    void sendMessage_Delegates() {
        MessageRequestDTO dto = new MessageRequestDTO();
        messageService.sendMessage(dto);
        verify(commandService).sendMessage(dto);
    }

    @Test
    @DisplayName("getMessageById - delegates to queryService")
    void getMessageById_Delegates() {
        messageService.getMessageById(1L);
        verify(queryService).getMessageById(1L);
    }

    @Test
    @DisplayName("getConversation - delegates to queryService")
    void getConversation_Delegates() {
        messageService.getConversation(100L, 200L);
        verify(queryService).getConversation(100L, 200L);
    }

    @Test
    @DisplayName("getConversation (paginated) - delegates to queryService")
    void getConversationPaginated_Delegates() {
        Pageable pageable = Pageable.unpaged();
        messageService.getConversation(100L, 200L, pageable);
        verify(queryService).getConversation(100L, 200L, pageable);
    }

    @Test
    @DisplayName("getConversationPartners - delegates to queryService")
    void getConversationPartners_Delegates() {
        messageService.getConversationPartners(100L);
        verify(queryService).getConversationPartners(100L);
    }

    @Test
    @DisplayName("getConversationPartners (paginated) - delegates to queryService")
    void getConversationPartnersPaginated_Delegates() {
        Pageable pageable = Pageable.unpaged();
        messageService.getConversationPartners(100L, pageable);
        verify(queryService).getConversationPartners(100L, pageable);
    }

    @Test
    @DisplayName("getConversationCursor - delegates to queryService")
    void getConversationCursor_Delegates() {
        messageService.getConversationCursor(100L, 200L, 1L, 2L, 20);
        verify(queryService).getConversationCursor(100L, 200L, 1L, 2L, 20);
    }
}
