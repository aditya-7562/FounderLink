package com.founderlink.messaging.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.founderlink.messaging.dto.MessageRequestDTO;
import com.founderlink.messaging.dto.MessageResponseDTO;
import com.founderlink.messaging.exception.GlobalExceptionHandler;
import com.founderlink.messaging.exception.MessageNotFoundException;
import com.founderlink.messaging.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MessageControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private MessageController messageController;

    private ObjectMapper objectMapper;

    private MessageResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(messageController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        responseDTO = MessageResponseDTO.builder()
                .id(1L)
                .senderId(100L)
                .receiverId(200L)
                .content("Hello!")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // --- POST /messages ---

    @Test
    @DisplayName("POST /messages - success returns 201")
    void sendMessage_WithValidRequest_Returns201() throws Exception {

        MessageRequestDTO request = new MessageRequestDTO();
        request.setSenderId(100L);
        request.setReceiverId(200L);
        request.setContent("Hello!");

        when(messageService.sendMessage(any(MessageRequestDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.senderId").value(100))
                .andExpect(jsonPath("$.receiverId").value(200))
                .andExpect(jsonPath("$.content").value("Hello!"));
    }

    @Test
    @DisplayName("POST /messages - missing senderId returns 400")
    void sendMessage_WithMissingSenderId_Returns400() throws Exception {

        MessageRequestDTO request = new MessageRequestDTO();
        request.setReceiverId(200L);
        request.setContent("Hello!");

        mockMvc.perform(post("/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /messages - blank content returns 400")
    void sendMessage_WithBlankContent_Returns400() throws Exception {

        MessageRequestDTO request = new MessageRequestDTO();
        request.setSenderId(100L);
        request.setReceiverId(200L);
        request.setContent(""); // important

        mockMvc.perform(post("/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // --- GET /messages/{id} ---

    @Test
    @DisplayName("GET /messages/{id} - success returns 200")
    void getMessageById_WhenFound_Returns200() throws Exception {
        when(messageService.getMessageById(1L)).thenReturn(responseDTO);

        mockMvc.perform(get("/messages/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.content").value("Hello!"));
    }

    @Test
    @DisplayName("GET /messages/{id} - not found returns 404")
    void getMessageById_WhenNotFound_Returns404() throws Exception {
        when(messageService.getMessageById(999L)).thenThrow(new MessageNotFoundException(999L));

        mockMvc.perform(get("/messages/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Message not found with id: 999"));
    }

    // --- GET /messages/conversation/{user1}/{user2} ---

    @Test
    @DisplayName("GET /messages/conversation/{user1}/{user2} - returns conversation")
    void getConversation_ReturnsMessages() throws Exception {
        MessageResponseDTO response2 = MessageResponseDTO.builder()
                .id(2L).senderId(200L).receiverId(100L)
                .content("Reply!").createdAt(LocalDateTime.now()).build();

        when(messageService.getConversation(100L, 200L))
                .thenReturn(Arrays.asList(responseDTO, response2));

        mockMvc.perform(get("/messages/conversation/100/200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].content").value("Hello!"))
                .andExpect(jsonPath("$[1].content").value("Reply!"));
    }

    // --- GET /messages/partners/{userId} ---

    @Test
    @DisplayName("GET /messages/partners/{userId} - returns partner IDs")
    void getConversationPartners_ReturnsPartnerIds() throws Exception {
        when(messageService.getConversationPartners(100L))
                .thenReturn(List.of(200L, 300L));

        mockMvc.perform(get("/messages/partners/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value(200))
                .andExpect(jsonPath("$[1]").value(300));
    }
}
