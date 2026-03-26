package com.founderlink.messaging.service;

import com.founderlink.messaging.client.UserServiceClient;
import com.founderlink.messaging.command.MessageCommandService;
import com.founderlink.messaging.dto.MessageRequestDTO;
import com.founderlink.messaging.dto.MessageResponseDTO;
import com.founderlink.messaging.dto.UserDTO;
import com.founderlink.messaging.entity.Message;
import com.founderlink.messaging.event.MessageEventPublisher;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    // ── Command side mocks ───────────────────────────────────────────────────

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private MessageEventPublisher messageEventPublisher;

    @InjectMocks
    private MessageCommandService messageCommandService;

    // ── Query side (shares messageRepository mock — same field name) ─────────

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

    // --- sendMessage tests ---

    @Test
    @DisplayName("sendMessage - success with valid users")
    void sendMessage_WithValidUsers_ReturnsResponseDTO() {
        when(userServiceClient.getUserById(100L)).thenReturn(senderDTO);
        when(userServiceClient.getUserById(200L)).thenReturn(receiverDTO);
        when(messageRepository.saveAndFlush(any(Message.class))).thenReturn(message1);

        MessageResponseDTO result = messageCommandService.sendMessage(validRequest);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getSenderId()).isEqualTo(100L);
        assertThat(result.getReceiverId()).isEqualTo(200L);
        assertThat(result.getContent()).isEqualTo("Hello from sender!");
        verify(userServiceClient).getUserById(100L);
        verify(userServiceClient).getUserById(200L);
        verify(messageRepository).saveAndFlush(any(Message.class));
    }

    @Test
    @DisplayName("sendMessage - throws when user-service returns null (user not found)")
    void sendMessage_WhenUserServiceReturnsNull_ThrowsException() {
        when(userServiceClient.getUserById(100L)).thenReturn(null);

        assertThatThrownBy(() -> messageCommandService.sendMessage(validRequest))
                .isInstanceOf(InvalidMessageException.class)
                .hasMessageContaining("does not exist");

        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("sendMessage - throws when sender equals receiver")
    void sendMessage_WhenSenderEqualsReceiver_ThrowsException() {
        MessageRequestDTO sameUserRequest = new MessageRequestDTO(100L, 100L, "Self message");

        assertThatThrownBy(() -> messageCommandService.sendMessage(sameUserRequest))
                .isInstanceOf(InvalidMessageException.class)
                .hasMessage("Sender and receiver cannot be the same user");

        verify(messageRepository, never()).save(any());
    }

    // --- sendMessageFallback test ---

    @Test
    @DisplayName("sendMessageFallback - throws when user-service is unavailable")
    void sendMessageFallback_ThrowsWhenServiceUnavailable() {
        assertThatThrownBy(() -> messageCommandService.sendMessageFallback(
                validRequest, new RuntimeException("Service unavailable")))
                .isInstanceOf(InvalidMessageException.class)
                .hasMessageContaining("User Service is unavailable");

        verify(messageRepository, never()).save(any());
    }

    // --- getConversation tests ---

    @Test
    @DisplayName("getConversation - returns conversation between two users")
    void getConversation_ReturnsMessages() {
        when(messageRepository.findConversation(100L, 200L))
                .thenReturn(Arrays.asList(message1, message2));

        List<MessageResponseDTO> result = messageQueryService.getConversation(100L, 200L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getContent()).isEqualTo("Hello from sender!");
        assertThat(result.get(1).getContent()).isEqualTo("Hello back from receiver!");
    }

    @Test
    @DisplayName("getConversation - returns empty list when no messages")
    void getConversation_WhenNoMessages_ReturnsEmptyList() {
        when(messageRepository.findConversation(100L, 300L)).thenReturn(List.of());

        List<MessageResponseDTO> result = messageQueryService.getConversation(100L, 300L);

        assertThat(result).isEmpty();
    }

    // --- getConversationPartners tests ---

    @Test
    @DisplayName("getConversationPartners - returns partner IDs")
    void getConversationPartners_ReturnsPartnerIds() {
        when(messageRepository.findConversationPartners(100L))
                .thenReturn(Arrays.asList(200L, 300L));

        List<Long> result = messageQueryService.getConversationPartners(100L);

        assertThat(result).containsExactly(200L, 300L);
    }

    // --- getMessageById tests ---

    @Test
    @DisplayName("getMessageById - returns message when found")
    void getMessageById_WhenFound_ReturnsDTO() {
        when(messageRepository.findById(1L)).thenReturn(Optional.of(message1));

        MessageResponseDTO result = messageQueryService.getMessageById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getContent()).isEqualTo("Hello from sender!");
    }

    @Test
    @DisplayName("getMessageById - throws when not found")
    void getMessageById_WhenNotFound_ThrowsException() {
        when(messageRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageQueryService.getMessageById(999L))
                .isInstanceOf(MessageNotFoundException.class)
                .hasMessageContaining("999");
    }

    // --- Fallback tests ---

    @Test
    @DisplayName("getConversationFallback - returns empty list")
    void getConversationFallback_ReturnsEmptyList() {
        List<MessageResponseDTO> result = messageQueryService.getConversationFallback(
                100L, 200L, new RuntimeException("fail"));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getConversationPartnersFallback - returns empty list")
    void getConversationPartnersFallback_ReturnsEmptyList() {
        List<Long> result = messageQueryService.getConversationPartnersFallback(
                100L, new RuntimeException("fail"));

        assertThat(result).isEmpty();
    }
}
