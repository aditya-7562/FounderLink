package com.founderlink.auth.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.founderlink.auth.dto.LoginRequest;
import com.founderlink.auth.dto.RegisterRequest;
import com.founderlink.auth.entity.Role;
import com.founderlink.auth.entity.User;
import com.founderlink.auth.exception.UserServiceUnavailableException;
import com.founderlink.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.config.import=",
        "spring.datasource.url=jdbc:h2:mem:authdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "jwt.secret=VGhpc0lzQVN0cm9uZ0pXVFNlY3JldEtleUZvclRlc3RzMTIzNDU2Nzg5MDEy",
        "resilience4j.retry.instances.userServiceSync.max-attempts=3",
        "resilience4j.retry.instances.userServiceSync.wait-duration=1ms",
        "resilience4j.retry.instances.userServiceSync.enable-exponential-backoff=false",
        "resilience4j.circuitbreaker.instances.userServiceSync.sliding-window-size=10",
        "resilience4j.circuitbreaker.instances.userServiceSync.minimum-number-of-calls=10"
})
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @MockBean
    private com.founderlink.auth.client.UserClient userClient;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void loginShouldAuthenticateAndReturnJwtAndRefreshCookie() throws Exception {
        User user = new User();
        user.setName("Alice Founder");
        user.setEmail("alice@founderlink.com");
        user.setPassword(passwordEncoder.encode("StrongPass1"));
        user.setRole(Role.FOUNDER);
        userRepository.save(user);

        LoginRequest request = new LoginRequest("alice@founderlink.com", "StrongPass1");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(jsonPath("$.email").value("alice@founderlink.com"))
                .andExpect(jsonPath("$.role").value("FOUNDER"))
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    void registerShouldFailAndRollbackWhenUserServiceIsDown() throws Exception {
        doThrow(new UserServiceUnavailableException("UserClient#createUser", "Service unavailable"))
                .when(userClient).createUser(any());

        RegisterRequest request = new RegisterRequest("Bob Founder", "bob@founderlink.com", "StrongPass1", Role.FOUNDER);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Dependent service is unavailable"))
                .andExpect(jsonPath("$.path").value("/auth/register"));

        org.assertj.core.api.Assertions.assertThat(userRepository.existsByEmail("bob@founderlink.com")).isFalse();
    }
}
