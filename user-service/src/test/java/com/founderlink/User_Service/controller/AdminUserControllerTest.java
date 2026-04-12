package com.founderlink.User_Service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.founderlink.User_Service.dto.UpdateStatusRequest;
import com.founderlink.User_Service.dto.UserResponseDto;
import com.founderlink.User_Service.entity.User;
import com.founderlink.User_Service.entity.UserStatus;
import com.founderlink.User_Service.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.Map;
import java.util.ArrayList;

@WebMvcTest(AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private ModelMapper modelMapper;

    @MockBean(name = "restTemplate")
    private RestTemplate restTemplate;

    @MockBean(name = "plainRestTemplate")
    private RestTemplate plainRestTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void updateStatus_AsAdmin_ShouldReturnOk() throws Exception {
        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus(UserStatus.SUSPENDED);

        User user = new User();
        user.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);
        when(modelMapper.map(any(), eq(UserResponseDto.class))).thenReturn(new UserResponseDto());

        mockMvc.perform(put("/users/admin/1/status")
                        .header("X-User-Role", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void updateStatus_SyncFailure_ShouldStillReturnOk() throws Exception {
        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus(UserStatus.BANNED);

        User user = new User();
        user.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);
        when(modelMapper.map(any(), eq(UserResponseDto.class))).thenReturn(new UserResponseDto());

        // Simulate RestTemplate error
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(), eq(Object.class)))
                .thenThrow(new RuntimeException("Sync failed"));

        mockMvc.perform(put("/users/admin/1/status")
                        .header("X-User-Role", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void updateStatus_AsNonAdmin_ShouldReturnForbidden() throws Exception {
        UpdateStatusRequest request = new UpdateStatusRequest();

        mockMvc.perform(put("/users/admin/1/status")
                        .header("X-User-Role", "ROLE_FOUNDER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void searchUsers_AsAdmin_ShouldReturnPage() throws Exception {
        when(userRepository.searchUsers(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new User())));

        mockMvc.perform(get("/users/admin/search")
                        .header("X-User-Role", "ROLE_ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void searchUsers_AsNonAdmin_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/users/admin/search")
                        .header("X-User-Role", "ROLE_INVESTOR"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMicroservicesHealth_AsAdmin_ShouldReturnOk() throws Exception {
        // Mock Prometheus response
        Map<String, Object> prometheusResp = new java.util.HashMap<>();
        Map<String, Object> data = new java.util.HashMap<>();
        List<Map<String, Object>> result = new ArrayList<>();
        
        Map<String, Object> service1 = new java.util.HashMap<>();
        service1.put("metric", Map.of("job", "user-service"));
        service1.put("value", List.of(123.456, "1"));
        result.add(service1);

        data.put("result", result);
        prometheusResp.put("data", data);

        when(plainRestTemplate.getForObject(contains("prometheus"), eq(Map.class)))
                .thenReturn(prometheusResp);

        mockMvc.perform(get("/users/admin/health/microservices")
                        .header("X-User-Role", "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("user-service"))
                .andExpect(jsonPath("$[0].status").value("UP"));
    }

    @Test
    void getMicroservicesHealth_AsNonAdmin_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/users/admin/health/microservices")
                        .header("X-User-Role", "ROLE_FOUNDER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMicroservicesHealth_PrometheusError_ShouldReturnEmptyList() throws Exception {
        when(plainRestTemplate.getForObject(contains("prometheus"), eq(Map.class)))
                .thenThrow(new RuntimeException("Prometheus down"));

        mockMvc.perform(get("/users/admin/health/microservices")
                        .header("X-User-Role", "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
