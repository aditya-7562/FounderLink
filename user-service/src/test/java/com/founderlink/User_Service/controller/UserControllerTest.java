package com.founderlink.User_Service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.founderlink.User_Service.dto.UserRequestAuthDto;
import com.founderlink.User_Service.dto.UserRequestDto;
import com.founderlink.User_Service.dto.UserResponseDto;
import com.founderlink.User_Service.entity.Role;
import com.founderlink.User_Service.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private UserController userController;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Since UserController uses @Value, we might need to inject it manually if context doesn't pick it up from properties
        // Actually @WebMvcTest should handle it if registered in test properties, but ReflectionTestUtils is safer if it's missing.
    }

    @Test
    void createUserInternal_WithValidSecret_ShouldReturnOk() throws Exception {
        ReflectionTestUtils.setField(userController, "internalSecret", "default-secret");
        
        UserRequestAuthDto dto = new UserRequestAuthDto();
        dto.setUserId(1L);
        dto.setEmail("internal@test.com");
        dto.setRole(Role.FOUNDER);

        UserResponseDto response = new UserResponseDto();
        response.setId(1L);
        response.setEmail("internal@test.com");

        when(userService.createUser(any())).thenReturn(response);

        mockMvc.perform(post("/users/internal")
                        .header("X-Auth-Source", "gateway")
                        .header("X-Internal-Secret", "default-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.email").value("internal@test.com"));
    }

    @Test
    void createUserInternal_WithInvalidSecret_ShouldReturnForbidden() throws Exception {
        ReflectionTestUtils.setField(userController, "internalSecret", "default-secret");
        
        UserRequestAuthDto dto = new UserRequestAuthDto();
        dto.setUserId(1L);
        dto.setEmail("valid@test.com");

        mockMvc.perform(post("/users/internal")
                        .header("X-Auth-Source", "wrong")
                        .header("X-Internal-Secret", "wrong")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUser_ShouldReturnUser() throws Exception {
        UserResponseDto response = new UserResponseDto();
        response.setId(1L);
        response.setEmail("user@test.com");

        when(userService.getUser(1L)).thenReturn(response);

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.email").value("user@test.com"));
    }

    @Test
    void updateUser_AsOwner_ShouldReturnOk() throws Exception {
        UserRequestDto dto = new UserRequestDto();
        UserResponseDto response = new UserResponseDto();
        response.setId(1L);

        when(userService.updateUser(eq(1L), any())).thenReturn(response);

        mockMvc.perform(put("/users/1")
                        .header("X-User-Id", 1L)
                        .header("X-User-Role", "ROLE_FOUNDER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    void updateUser_AsAdmin_ShouldReturnOk() throws Exception {
        UserRequestDto dto = new UserRequestDto();
        UserResponseDto response = new UserResponseDto();
        response.setId(1L);

        when(userService.updateUser(eq(1L), any())).thenReturn(response);

        mockMvc.perform(put("/users/1")
                        .header("X-User-Id", 99L)
                        .header("X-User-Role", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    void updateUser_AsNonOwnerNonAdmin_ShouldReturnForbidden() throws Exception {
        UserRequestDto dto = new UserRequestDto();

        mockMvc.perform(put("/users/1")
                        .header("X-User-Id", 99L)
                        .header("X-User-Role", "ROLE_FOUNDER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllUsers_WithoutPagination_ShouldReturnList() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(new UserResponseDto()));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getAllUsers_WithPagination_ShouldReturnPaginated() throws Exception {
        when(userService.getAllUsers(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(new UserResponseDto())));

        mockMvc.perform(get("/users?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void getUsersByRole_ShouldReturnUsers() throws Exception {
        when(userService.getUsersByRole(eq(Role.FOUNDER))).thenReturn(List.of(new UserResponseDto()));

        mockMvc.perform(get("/users/role/FOUNDER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getUsersByRole_WithPrefix_ShouldReturnUsers() throws Exception {
        when(userService.getUsersByRole(eq(Role.FOUNDER))).thenReturn(List.of(new UserResponseDto()));

        mockMvc.perform(get("/users/role/ROLE_FOUNDER"))
                .andExpect(status().isOk());
    }

    @Test
    void getUsersByRole_Admin_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/users/role/ADMIN"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUsersByRole_Invalid_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/users/role/INVALID_ROLE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPublicStats_ShouldReturnStats() throws Exception {
        when(userService.countByRole(any())).thenReturn(10L);

        mockMvc.perform(get("/users/public/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.founders").value(10));
    }
}
