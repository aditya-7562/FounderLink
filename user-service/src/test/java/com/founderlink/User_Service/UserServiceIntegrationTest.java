package com.founderlink.User_Service;

import com.founderlink.User_Service.dto.UserRequestAuthDto;
import com.founderlink.User_Service.dto.UserRequestDto;
import com.founderlink.User_Service.dto.UserResponseDto;
import com.founderlink.User_Service.entity.Role;
import com.founderlink.User_Service.entity.User;
import com.founderlink.User_Service.exceptions.ConflictException;
import com.founderlink.User_Service.exceptions.UserNotFoundException;
import com.founderlink.User_Service.repository.UserRepository;
import com.founderlink.User_Service.service.UserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Test
    void should_create_user_when_not_exists() {
        UserRequestAuthDto request = authRequest(101L, "Alice", "alice@founderlink.com", Role.FOUNDER);
        request.setSkills("Java, Spring");
        request.setExperience("5 years");
        request.setBio("Backend engineer");
        request.setPortfolioLinks("https://portfolio.example/alice");

        UserResponseDto response = userService.createUser(request);

        User savedUser = reloadedUser(101L);
        assertThat(response.getId()).isEqualTo(101L);
        assertThat(savedUser.getEmail()).isEqualTo("alice@founderlink.com");
        assertThat(savedUser.getRole()).isEqualTo(Role.FOUNDER);
        assertThat(savedUser.getSkills()).isEqualTo("Java, Spring");
        assertThat(savedUser.getUpdatedAt()).isNotNull();
    }

    @Test
    void should_return_existing_user_if_same_id_and_same_data() {
        User existingUser = existingUser(200L, "existing@founderlink.com", Role.INVESTOR);
        existingUser.setName("Existing User");
        existingUser.setSkills("Angel investing");
        userRepository.saveAndFlush(existingUser);

        UserRequestAuthDto request = authRequest(200L, "Existing User", "existing@founderlink.com", Role.INVESTOR);
        request.setSkills("Angel investing");

        UserResponseDto response = userService.createUser(request);

        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(response.getId()).isEqualTo(200L);
        assertThat(response.getEmail()).isEqualTo("existing@founderlink.com");
        assertThat(reloadedUser(200L).getSkills()).isEqualTo("Angel investing");
    }

    @ParameterizedTest
    @MethodSource("conflictingCreateRequests")
    void should_throw_conflict_if_same_id_but_different_email_or_role(UserRequestAuthDto request) {
        userRepository.saveAndFlush(existingUser(300L, "owner@founderlink.com", Role.COFOUNDER));

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("User identity data does not match existing record.");

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void should_update_only_non_null_fields() {
        userRepository.saveAndFlush(existingUser(400L, "profile@founderlink.com", Role.FOUNDER));
        LocalDateTime beforeUpdate = reloadedUser(400L).getUpdatedAt();

        UserRequestDto update = new UserRequestDto();
        update.setName("Updated Name");
        update.setBio("Updated bio");

        userService.updateUser(400L, update);

        User updatedUser = reloadedUser(400L);
        assertThat(updatedUser.getName()).isEqualTo("Updated Name");
        assertThat(updatedUser.getBio()).isEqualTo("Updated bio");
        assertThat(updatedUser.getSkills()).isEqualTo("Spring");
        assertThat(updatedUser.getExperience()).isEqualTo("7 years");
        assertThat(updatedUser.getPortfolioLinks()).isEqualTo("https://portfolio.example/original");
        assertThat(updatedUser.getEmail()).isEqualTo("profile@founderlink.com");
        assertThat(updatedUser.getRole()).isEqualTo(Role.FOUNDER);
        assertThat(updatedUser.getUpdatedAt()).isAfter(beforeUpdate);
    }

    @Test
    void should_not_overwrite_existing_fields_with_null() {
        userRepository.saveAndFlush(existingUser(401L, "nullsafe@founderlink.com", Role.INVESTOR));

        UserRequestDto update = new UserRequestDto();
        update.setName(null);
        update.setSkills(null);
        update.setExperience(null);
        update.setBio("Only bio changes");
        update.setPortfolioLinks(null);

        userService.updateUser(401L, update);

        User updatedUser = reloadedUser(401L);
        assertThat(updatedUser.getName()).isEqualTo("Original Name");
        assertThat(updatedUser.getSkills()).isEqualTo("Spring");
        assertThat(updatedUser.getExperience()).isEqualTo("7 years");
        assertThat(updatedUser.getPortfolioLinks()).isEqualTo("https://portfolio.example/original");
        assertThat(updatedUser.getBio()).isEqualTo("Only bio changes");
    }

    @Test
    void should_not_update_email_even_if_present_in_request() throws Exception {
        userRepository.saveAndFlush(existingUser(500L, "safe@founderlink.com", Role.FOUNDER));

        mockMvc.perform(put("/users/{id}", 500L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Safe Update",
                                  "email": "attacker@evil.com"
                                }
                                """))
                .andExpect(status().isOk());

        User updatedUser = reloadedUser(500L);
        assertThat(updatedUser.getName()).isEqualTo("Safe Update");
        assertThat(updatedUser.getEmail()).isEqualTo("safe@founderlink.com");
    }

    @Test
    void should_not_update_role_even_if_present_in_request() throws Exception {
        userRepository.saveAndFlush(existingUser(501L, "role@founderlink.com", Role.INVESTOR));

        mockMvc.perform(put("/users/{id}", 501L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bio": "Role update attempt",
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isOk());

        User updatedUser = reloadedUser(501L);
        assertThat(updatedUser.getBio()).isEqualTo("Role update attempt");
        assertThat(updatedUser.getRole()).isEqualTo(Role.INVESTOR);
    }

    @Test
    void should_not_update_id_from_request_body() throws Exception {
        userRepository.saveAndFlush(existingUser(502L, "id@founderlink.com", Role.COFOUNDER));

        mockMvc.perform(put("/users/{id}", 502L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": 9999,
                                  "name": "ID Mutation Attempt"
                                }
                                """))
                .andExpect(status().isOk());

        User originalUser = reloadedUser(502L);
        Optional<User> unexpectedUser = userRepository.findById(9999L);
        assertThat(originalUser.getName()).isEqualTo("ID Mutation Attempt");
        assertThat(originalUser.getId()).isEqualTo(502L);
        assertThat(unexpectedUser).isEmpty();
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void should_fail_when_updating_non_existing_user() {
        UserRequestDto update = new UserRequestDto();
        update.setName("Ghost");

        assertThatThrownBy(() -> userService.updateUser(999L, update))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found.");
    }

    @Test
    void should_not_allow_mass_assignment_behavior() throws Exception {
        userRepository.saveAndFlush(existingUser(503L, "mass@founderlink.com", Role.FOUNDER));

        mockMvc.perform(put("/users/{id}", 503L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "skills": "Distributed systems",
                                  "authorities": ["ADMIN"],
                                  "isAdmin": true,
                                  "createdBy": "attacker"
                                }
                                """))
                .andExpect(status().isOk());

        User updatedUser = reloadedUser(503L);
        assertThat(updatedUser.getSkills()).isEqualTo("Distributed systems");
        assertThat(updatedUser.getEmail()).isEqualTo("mass@founderlink.com");
        assertThat(updatedUser.getRole()).isEqualTo(Role.FOUNDER);
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void should_fail_when_duplicate_email_inserted() {
        userService.createUser(authRequest(600L, "First", "duplicate@founderlink.com", Role.FOUNDER));

        assertThatThrownBy(() -> {
            userService.createUser(authRequest(601L, "Second", "duplicate@founderlink.com", Role.INVESTOR));
            userRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void should_preserve_existing_data_on_partial_update() {
        userRepository.saveAndFlush(existingUser(700L, "partial@founderlink.com", Role.FOUNDER));

        UserRequestDto update = new UserRequestDto();
        update.setExperience("10 years");

        userService.updateUser(700L, update);

        User updatedUser = reloadedUser(700L);
        assertThat(updatedUser.getExperience()).isEqualTo("10 years");
        assertThat(updatedUser.getName()).isEqualTo("Original Name");
        assertThat(updatedUser.getSkills()).isEqualTo("Spring");
        assertThat(updatedUser.getBio()).isEqualTo("Original bio");
        assertThat(updatedUser.getPortfolioLinks()).isEqualTo("https://portfolio.example/original");
        assertThat(updatedUser.getEmail()).isEqualTo("partial@founderlink.com");
        assertThat(updatedUser.getRole()).isEqualTo(Role.FOUNDER);
    }

    @Test
    void update_with_empty_dto_no_changes() {
        userRepository.saveAndFlush(existingUser(701L, "empty@founderlink.com", Role.INVESTOR));
        User beforeUpdate = reloadedUser(701L);
        String nameBefore = beforeUpdate.getName();
        String skillsBefore = beforeUpdate.getSkills();
        String experienceBefore = beforeUpdate.getExperience();
        String bioBefore = beforeUpdate.getBio();
        String portfolioBefore = beforeUpdate.getPortfolioLinks();
        String emailBefore = beforeUpdate.getEmail();
        Role roleBefore = beforeUpdate.getRole();
        LocalDateTime beforeTimestamp = beforeUpdate.getUpdatedAt();

        userService.updateUser(701L, new UserRequestDto());

        User updatedUser = reloadedUser(701L);
        assertThat(updatedUser.getName()).isEqualTo(nameBefore);
        assertThat(updatedUser.getSkills()).isEqualTo(skillsBefore);
        assertThat(updatedUser.getExperience()).isEqualTo(experienceBefore);
        assertThat(updatedUser.getBio()).isEqualTo(bioBefore);
        assertThat(updatedUser.getPortfolioLinks()).isEqualTo(portfolioBefore);
        assertThat(updatedUser.getEmail()).isEqualTo(emailBefore);
        assertThat(updatedUser.getRole()).isEqualTo(roleBefore);
        assertThat(updatedUser.getUpdatedAt()).isAfter(beforeTimestamp);
    }

    @Test
    void update_with_only_one_field_only_that_field_changes() {
        userRepository.saveAndFlush(existingUser(702L, "onefield@founderlink.com", Role.COFOUNDER));

        UserRequestDto update = new UserRequestDto();
        update.setPortfolioLinks("https://portfolio.example/new");

        userService.updateUser(702L, update);

        User updatedUser = reloadedUser(702L);
        assertThat(updatedUser.getPortfolioLinks()).isEqualTo("https://portfolio.example/new");
        assertThat(updatedUser.getName()).isEqualTo("Original Name");
        assertThat(updatedUser.getSkills()).isEqualTo("Spring");
        assertThat(updatedUser.getExperience()).isEqualTo("7 years");
        assertThat(updatedUser.getBio()).isEqualTo("Original bio");
        assertThat(updatedUser.getEmail()).isEqualTo("onefield@founderlink.com");
        assertThat(updatedUser.getRole()).isEqualTo(Role.COFOUNDER);
    }

    private static Stream<UserRequestAuthDto> conflictingCreateRequests() {
        UserRequestAuthDto emailMismatch = authRequest(300L, "Owner", "different@founderlink.com", Role.COFOUNDER);
        UserRequestAuthDto roleMismatch = authRequest(300L, "Owner", "owner@founderlink.com", Role.ADMIN);
        return Stream.of(emailMismatch, roleMismatch);
    }

    private static UserRequestAuthDto authRequest(Long id, String name, String email, Role role) {
        UserRequestAuthDto request = new UserRequestAuthDto();
        request.setUserId(id);
        request.setName(name);
        request.setEmail(email);
        request.setRole(role);
        return request;
    }

    private static User existingUser(Long id, String email, Role role) {
        User user = new User();
        user.setId(id);
        user.setName("Original Name");
        user.setEmail(email);
        user.setRole(role);
        user.setSkills("Spring");
        user.setExperience("7 years");
        user.setBio("Original bio");
        user.setPortfolioLinks("https://portfolio.example/original");
        user.setUpdatedAt(LocalDateTime.now().minusDays(1));
        return user;
    }

    private User reloadedUser(Long id) {
        entityManager.flush();
        entityManager.clear();
        return userRepository.findById(id).orElseThrow();
    }
}
