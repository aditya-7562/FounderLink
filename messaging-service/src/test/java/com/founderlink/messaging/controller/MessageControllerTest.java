package com.founderlink.messaging.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.founderlink.messaging.dto.CursorPageDTO;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    private ObjectMapper objectMapper = new ObjectMapper();
    private MessageResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(messageController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        responseDTO = MessageResponseDTO.builder()
                .id(1L)
                .senderId(100L)
                .receiverId(200L)
                .content("Hello!")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("POST /messages - Success")
    void sendMessage_Success() throws Exception {
        MessageRequestDTO request = new MessageRequestDTO(100L, 200L, "Hello!");
        when(messageService.sendMessage(any())).thenReturn(responseDTO);

        mockMvc.perform(post("/messages")
                        .header("X-User-Id", 100L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("GET /messages/{id} - Success")
    void getMessageById_Success() throws Exception {
        when(messageService.getMessageById(1L)).thenReturn(responseDTO);

        mockMvc.perform(get("/messages/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /messages/{id} - Not Found")
    void getMessageById_NotFound() throws Exception {
        when(messageService.getMessageById(99L)).thenThrow(new MessageNotFoundException(99L));

        mockMvc.perform(get("/messages/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /conversation - Success variants")
    void getConversation_Success() throws Exception {
        when(messageService.getConversation(100L, 200L)).thenReturn(List.of(responseDTO));

        // Particpant 1
        mockMvc.perform(get("/messages/conversation/100/200")
                        .header("X-User-Id", 100L))
                .andExpect(status().isOk());

        // Participant 2
        mockMvc.perform(get("/messages/conversation/100/200")
                        .header("X-User-Id", 200L))
                .andExpect(status().isOk());

        // Admin
        mockMvc.perform(get("/messages/conversation/100/200")
                        .header("X-User-Id", 500L)
                        .header("X-User-Role", "ROLE_ADMIN"))
                .andExpect(status().isOk());

        // No header (bypass check)
        mockMvc.perform(get("/messages/conversation/100/200"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /conversation - Forbidden")
    void getConversation_Forbidden() throws Exception {
        mockMvc.perform(get("/messages/conversation/100/200")
                        .header("X-User-Id", 300L)
                        .header("X-User-Role", "ROLE_USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /conversation - Paginated success")
    void getConversation_Page() throws Exception {
        when(messageService.getConversation(eq(100L), eq(200L), any())).thenReturn(new PageImpl<>(List.of(responseDTO)));

        // Full params
        mockMvc.perform(get("/messages/conversation/100/200")
                        .header("X-User-Id", 100L)
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "id,asc"))
                .andExpect(status().isOk());

        // Partial params
        mockMvc.perform(get("/messages/conversation/100/200")
                        .header("X-User-Id", 100L)
                        .param("page", "0"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /conversation - Paginated sorting variants")
    void getConversation_PageSorting() throws Exception {
        when(messageService.getConversation(eq(100L), eq(200L), any())).thenReturn(new PageImpl<>(List.of(responseDTO)));

        // default desc
        mockMvc.perform(get("/messages/conversation/100/200")
                        .header("X-User-Id", 100L)
                        .param("page", "0")
                        .param("sort", "id"))
                .andExpect(status().isOk());

        // invalid prop (comma only handled by my hardening)
        mockMvc.perform(get("/messages/conversation/100/200")
                        .header("X-User-Id", 100L)
                        .param("page", "0")
                        .param("sort", ",")) 
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /partners - Success variants")
    void getPartners() throws Exception {
        when(messageService.getConversationPartners(100L)).thenReturn(List.of(200L));

        // Owner
        mockMvc.perform(get("/messages/partners/100")
                        .header("X-User-Id", 100L))
                .andExpect(status().isOk());

        // Admin
        mockMvc.perform(get("/messages/partners/100")
                        .header("X-User-Role", "ROLE_ADMIN"))
                .andExpect(status().isOk());

        // No header
        mockMvc.perform(get("/messages/partners/100"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /partners - Forbidden")
    void getPartners_Forbidden() throws Exception {
        mockMvc.perform(get("/messages/partners/100")
                        .header("X-User-Id", 200L))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /partners - Paginated")
    void getPartners_Page() throws Exception {
        when(messageService.getConversationPartners(eq(100L), any())).thenReturn(new PageImpl<>(List.of(200L)));

        mockMvc.perform(get("/messages/partners/100")
                        .header("X-User-Id", 100L)
                        .param("page", "0"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /cursor - Success variants")
    void getCursor() throws Exception {
        CursorPageDTO<MessageResponseDTO> page = CursorPageDTO.<MessageResponseDTO>builder()
                .content(List.of(responseDTO)).build();
        when(messageService.getConversationCursor(100L, 200L, null, null, 20)).thenReturn(page);

        // Owner
        mockMvc.perform(get("/messages/conversation/100/200/cursor")
                        .header("X-User-Id", 100L))
                .andExpect(status().isOk());

        // Admin
        mockMvc.perform(get("/messages/conversation/100/200/cursor")
                        .header("X-User-Role", "ROLE_ADMIN"))
                .andExpect(status().isOk());

        // No header
        mockMvc.perform(get("/messages/conversation/100/200/cursor"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /cursor - Forbidden")
    void getCursor_Forbidden() throws Exception {
        mockMvc.perform(get("/messages/conversation/100/200/cursor")
                        .header("X-User-Id", 300L))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /cursor - Bad Request")
    void getCursor_Bad() throws Exception {
        mockMvc.perform(get("/messages/conversation/100/200/cursor")
                        .header("X-User-Id", 100L)
                        .param("before", "1")
                        .param("after", "2"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Sanitization and Boundary Edge Cases")
    void sanitization() throws Exception {
        when(messageService.getConversation(eq(100L), eq(200L), any())).thenReturn(new PageImpl<>(List.of(responseDTO)));

        // safePage Math.max(page, 0)
        mockMvc.perform(get("/messages/conversation/100/200")
                        .header("X-User-Id", 100L)
                        .param("page", "-5"))
                .andExpect(status().isOk());

        // safeSize clipping (size=0 -> 1, size=100 -> 50)
        mockMvc.perform(get("/messages/conversation/100/200")
                        .header("X-User-Id", 100L)
                        .param("page", "0")
                        .param("size", "0"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/messages/conversation/100/200")
                        .header("X-User-Id", 100L)
                        .param("page", "0")
                        .param("size", "100"))
                .andExpect(status().isOk());
    }
}
