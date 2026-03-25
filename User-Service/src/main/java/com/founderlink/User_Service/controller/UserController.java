package com.founderlink.User_Service.controller;

import com.founderlink.User_Service.dto.UserRequestAuthDto;
import com.founderlink.User_Service.dto.UserRequestDto;
import com.founderlink.User_Service.dto.UserResponseDto;
import com.founderlink.User_Service.entity.Role;
import com.founderlink.User_Service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
// Use fully qualified name for @ApiResponse annotation to avoid collision

import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "APIs for managing users")
public class UserController {

    private final UserService service;

    @Value("${internal.secret:my-founderlink-internal-secret-2024}")
    private String internalSecret;

    private static final String AUTH_SOURCE_HEADER = "X-Auth-Source";
    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";
    private static final String EXPECTED_AUTH_SOURCE = "gateway";

    // Adding Users
    @Operation(summary = "Create user (internal)", description = "Creates a new user via internal endpoint.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User created successfully")
    @PostMapping("/internal")
    public ResponseEntity<UserResponseDto> createUser(
            @Valid @RequestBody UserRequestAuthDto dto,
            @RequestHeader(name = AUTH_SOURCE_HEADER, required = false) String authSource,
            @RequestHeader(name = INTERNAL_SECRET_HEADER, required = false) String secret) {

        // Validate internal endpoint access
        if (!isValidInternalAccess(authSource, secret)) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(service.createUser(dto));
    }

    @Operation(summary = "Get user by ID", description = "Fetches a user by their ID.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User fetched successfully")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(service.getUser(id));
    }

        @Operation(summary = "Update user", description = "Updates a user's information.")
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User updated successfully")
        @PutMapping("/{id}")
        public ResponseEntity<UserResponseDto> updateUser(@PathVariable Long id,
            @RequestBody UserRequestDto userRequestDto) {
        return ResponseEntity.ok(service.updateUser(id, userRequestDto));
    }

    @Operation(summary = "Get all users", description = "Fetches all users.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Users fetched successfully")
    @GetMapping
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        return ResponseEntity.ok(service.getAllUsers());
    }

        @Operation(summary = "Get users by role", description = "Fetches users by their role.")
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Users fetched successfully")
        @GetMapping("/role")
        public ResponseEntity<List<UserResponseDto>> getUsersByRole(
            @RequestHeader(name = "X-User-Role") String roleHeader) {

        log.info("GET /users/role - fetching users by role: {}", roleHeader);

        try {
            String roleName = roleHeader.toUpperCase().replace("ROLE_", "");
            Role role = Role.valueOf(roleName);

            if (role == Role.ADMIN) {
                log.warn("Attempt to fetch ADMIN users - blocked");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Collections.emptyList());
            }

            List<UserResponseDto> response = service.getUsersByRole(role);
            log.info("Successfully fetched {} users with role: {}", response.size(), roleHeader);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid role provided: {}", roleHeader);
            return ResponseEntity.badRequest().body(Collections.emptyList());
        }
    }

    private boolean isValidInternalAccess(String authSource, String secret) {
        // Both headers must be present and correct
        if (authSource == null || secret == null) {
            return false;
        }

        return EXPECTED_AUTH_SOURCE.equals(authSource) && internalSecret.equals(secret);
    }
}
