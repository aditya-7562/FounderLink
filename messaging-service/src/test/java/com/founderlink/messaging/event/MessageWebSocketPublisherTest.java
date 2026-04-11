package com.founderlink.messaging.event;

import com.founderlink.messaging.dto.MessageResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MessageWebSocketPublisherTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private MessageWebSocketPublisher messageWebSocketPublisher;

    @Test
    @DisplayName("pushToConversation - calls messagingTemplate with correct destination")
    void pushToConversation_Success() {
        MessageResponseDTO dto = MessageResponseDTO.builder().id(1L).content("Hi").build();
        messageWebSocketPublisher.pushToConversation(100L, 200L, dto);

        // Normalized topic for (100, 200) should be /topic/conversation/100/200
        verify(messagingTemplate).convertAndSend(eq("/topic/conversation/100/200"), eq(dto));
    }

    @Test
    @DisplayName("pushToConversation - handles reversed IDs correctly (normalization)")
    void pushToConversation_NormalizationWorks() {
        MessageResponseDTO dto = MessageResponseDTO.builder().id(1L).content("Hi").build();
        messageWebSocketPublisher.pushToConversation(200L, 100L, dto);

        // Should still be /topic/conversation/100/200
        verify(messagingTemplate).convertAndSend(eq("/topic/conversation/100/200"), eq(dto));
    }

    @Test
    @DisplayName("pushToConversation - handles exception gracefully")
    void pushToConversation_HandlesException() {
        MessageResponseDTO dto = MessageResponseDTO.builder().id(1L).content("Hi").build();
        doThrow(new RuntimeException("WS Error")).when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        // Should not throw exception
        messageWebSocketPublisher.pushToConversation(100L, 200L, dto);

        verify(messagingTemplate).convertAndSend(eq("/topic/conversation/100/200"), eq(dto));
    }
}
