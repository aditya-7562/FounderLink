package com.founderlink.messaging.service;

import com.founderlink.messaging.client.UserServiceClient;
import com.founderlink.messaging.command.MessageCommandService;
import com.founderlink.messaging.dto.CursorPageDTO;
import com.founderlink.messaging.dto.MessageRequestDTO;
import com.founderlink.messaging.dto.MessageResponseDTO;
import com.founderlink.messaging.dto.UserDTO;
import com.founderlink.messaging.entity.Message;
import com.founderlink.messaging.event.MessageEventPublisher;
import com.founderlink.messaging.event.MessageWebSocketPublisher;
import com.founderlink.messaging.exception.InvalidMessageException;
import com.founderlink.messaging.exception.MessageNotFoundException;
import com.founderlink.messaging.query.MessageQueryService;
import com.founderlink.messaging.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private MessageEventPublisher messageEventPublisher;

    @Mock
    private MessageWebSocketPublisher wsPublisher;

    @InjectMocks
    private MessageCommandService messageCommandService;

    @InjectMocks
    private MessageQueryService messageQueryService;

    private Message message1;
    private Message message2;
    private MessageRequestDTO validRequest;
    private UserDTO senderDTO;
    private UserDTO receiverDTO;

    @BeforeEach
    void setUp() {
        message1 = new Message();
        message1.setId(1L);
        message1.setSenderId(100L);
        message1.setReceiverId(200L);
        message1.setContent("Hello from sender!");
        message1.setCreatedAt(LocalDateTime.now());

        message2 = new Message();
        message2.setId(2L);
        message2.setSenderId(200L);
        message2.setReceiverId(100L);
        message2.setContent("Hello back from receiver!");
        message2.setCreatedAt(LocalDateTime.now());

        validRequest = new MessageRequestDTO(100L, 200L, "Hello from sender!");

        senderDTO = new UserDTO(1L, 100L, "Sender User", "sender@test.com");
        receiverDTO = new UserDTO(2L, 200L, "Receiver User", "receiver@test.com");
    }

    @Test
    @DisplayName("sendMessage - success")
    void sendMessage_Success() {
        when(userServiceClient.getUserById(100L)).thenReturn(senderDTO);
        when(userServiceClient.getUserById(200L)).thenReturn(receiverDTO);
        when(messageRepository.saveAndFlush(any(Message.class))).thenReturn(message1);

        MessageResponseDTO result = messageCommandService.sendMessage(validRequest);

        assertThat(result).isNotNull();
        verify(wsPublisher).pushToConversation(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("sendMessage - same user throws")
    void sendMessage_SameUser_Throws() {
        MessageRequestDTO same = new MessageRequestDTO(100L, 100L, "Hi");
        assertThatThrownBy(() -> messageCommandService.sendMessage(same))
                .isInstanceOf(InvalidMessageException.class);
    }

    @Test
    @DisplayName("sendMessage - sender null throws")
    void sendMessage_SenderNull_Throws() {
        when(userServiceClient.getUserById(100L)).thenReturn(null);
        assertThatThrownBy(() -> messageCommandService.sendMessage(validRequest))
                .isInstanceOf(InvalidMessageException.class);
    }

    @Test
    @DisplayName("sendMessage - receiver null throws")
    void sendMessage_ReceiverNull_Throws() {
        when(userServiceClient.getUserById(100L)).thenReturn(senderDTO);
        when(userServiceClient.getUserById(200L)).thenReturn(null);
        assertThatThrownBy(() -> messageCommandService.sendMessage(validRequest))
                .isInstanceOf(InvalidMessageException.class);
    }

    @Test
    @DisplayName("sendMessage - null sender name uses default")
    void sendMessage_NullSenderName_UsesDefault() {
        senderDTO.setName(null);
        when(userServiceClient.getUserById(100L)).thenReturn(senderDTO);
        when(userServiceClient.getUserById(200L)).thenReturn(receiverDTO);
        when(messageRepository.saveAndFlush(any(Message.class))).thenReturn(message1);

        messageCommandService.sendMessage(validRequest);

        verify(messageEventPublisher).publishMessageSent(anyLong(), anyLong(), anyLong(), eq("Someone"));
    }

    @Test
    @DisplayName("sendMessage - handles publication error")
    void sendMessage_HandlesPubError() {
        when(userServiceClient.getUserById(100L)).thenReturn(senderDTO);
        when(userServiceClient.getUserById(200L)).thenReturn(receiverDTO);
        when(messageRepository.saveAndFlush(any(Message.class))).thenReturn(message1);
        doThrow(new RuntimeException("Err")).when(messageEventPublisher).publishMessageSent(anyLong(), anyLong(), anyLong(), anyString());

        MessageResponseDTO result = messageCommandService.sendMessage(validRequest);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("sendMessageFallback - throws")
    void sendMessageFallback_Throws() {
        assertThatThrownBy(() -> messageCommandService.sendMessageFallback(validRequest, new RuntimeException()))
                .isInstanceOf(InvalidMessageException.class);
    }

    @Test
    @DisplayName("getConversation - returns list")
    void getConversation_ReturnsList() {
        when(messageRepository.findConversation(100L, 200L)).thenReturn(List.of(message1));
        List<MessageResponseDTO> result = messageQueryService.getConversation(100L, 200L);
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("getConversationPartners - returns list")
    void getConversationPartners_ReturnsList() {
        when(messageRepository.findConversationPartners(100L)).thenReturn(List.of(200L));
        List<Long> result = messageQueryService.getConversationPartners(100L);
        assertThat(result).containsExactly(200L);
    }

    @Test
    @DisplayName("getMessageById - returns dto")
    void getMessageById_ReturnsDto() {
        when(messageRepository.findById(1L)).thenReturn(Optional.of(message1));
        MessageResponseDTO result = messageQueryService.getMessageById(1L);
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getConversationCursor - initial")
    void getConversationCursor_Initial() {
        when(messageRepository.findBefore(eq(100L), eq(200L), anyLong(), any())).thenReturn(new ArrayList<>(List.of(message2, message1)));
        CursorPageDTO<MessageResponseDTO> result = messageQueryService.getConversationCursor(100L, 200L, null, null, 20);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getConversationCursor - before")
    void getConversationCursor_Before() {
        when(messageRepository.findBefore(eq(100L), eq(200L), eq(2L), any())).thenReturn(new ArrayList<>(List.of(message1)));
        CursorPageDTO<MessageResponseDTO> result = messageQueryService.getConversationCursor(100L, 200L, 2L, null, 10);
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getConversationCursor - after")
    void getConversationCursor_After() {
        when(messageRepository.findAfter(eq(100L), eq(200L), eq(1L), any())).thenReturn(List.of(message2));
        CursorPageDTO<MessageResponseDTO> result = messageQueryService.getConversationCursor(100L, 200L, null, 1L, 10);
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getConversationCursor - empty")
    void getConversationCursor_Empty() {
        when(messageRepository.findBefore(eq(100L), eq(200L), anyLong(), any())).thenReturn(List.of());
        CursorPageDTO<MessageResponseDTO> result = messageQueryService.getConversationCursor(100L, 200L, null, null, 10);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("getConversationCursor - size clipping logic")
    void getConversationCursor_SizeClipping() {
        when(messageRepository.findBefore(eq(100L), eq(200L), anyLong(), any())).thenReturn(new ArrayList<>());
        
        // size 0 -> safeSize 1
        messageQueryService.getConversationCursor(100L, 200L, null, null, 0);
        verify(messageRepository).findBefore(eq(100L), eq(200L), anyLong(), argThat(p -> p.getPageSize() == 1));

        // size 100 -> safeSize 50
        messageQueryService.getConversationCursor(100L, 200L, null, null, 100);
        verify(messageRepository).findBefore(eq(100L), eq(200L), anyLong(), argThat(p -> p.getPageSize() == 50));
    }

    @Test
    @DisplayName("Fallbacks")
    void fallbacks() {
        assertThat(messageQueryService.getConversationFallback(1L, 2L, new RuntimeException())).isEmpty();
        assertThat(messageQueryService.getConversationPartnersFallback(1L, new RuntimeException())).isEmpty();
    }

    @Test
    @DisplayName("Paginated")
    void paginated() {
        Pageable p = PageRequest.of(0, 10);
        when(messageRepository.findConversationPage(eq(1L), eq(2L), eq(p))).thenReturn(new PageImpl<>(List.of(message1)));
        assertThat(messageQueryService.getConversation(1L, 2L, p)).hasSize(1);

        when(messageRepository.findConversationPartnersPage(eq(1L), eq(p))).thenReturn(new PageImpl<>(List.of(2L)));
        assertThat(messageQueryService.getConversationPartners(1L, p)).hasSize(1);
    }
}
