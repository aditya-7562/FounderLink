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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "APIs for managing users")
public class UserController {

    private final UserService service;

    @Value("${internal.secret}")
    private String internalSecret;

    private static final String AUTH_SOURCE_HEADER = "X-Auth-Source";
    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";
    private static final String EXPECTED_AUTH_SOURCE = "gateway";

    @Operation(summary = "Create user (internal)", description = "Creates a new user via internal endpoint.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed — invalid request body"),
            @ApiResponse(responseCode = "403", description = "Forbidden — invalid internal secret"),
            @ApiResponse(responseCode = "409", description = "User already exists")
    })
    @PostMapping("/internal")
    public ResponseEntity<UserResponseDto> createUser(
            @Valid @RequestBody UserRequestAuthDto dto,
            @RequestHeader(name = AUTH_SOURCE_HEADER, required = false) String authSource,
            @RequestHeader(name = INTERNAL_SECRET_HEADER, required = false) String secret) {

        if (!isValidInternalAccess(authSource, secret)) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(service.createUser(dto));
    }

    @Operation(summary = "Get user by ID", description = "Fetches a user by their ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User fetched successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(service.getUser(id));
    }

    @Operation(summary = "Update user", description = "Updates a user's information.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User updated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "409", description = "Conflict — duplicate data")
    })
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDto> updateUser(
            @RequestHeader("X-User-Id") Long authenticatedUserId,
            @RequestHeader("X-User-Role") String userRole,
            @PathVariable Long id,
            @RequestBody UserRequestDto userRequestDto) {
        if (!authenticatedUserId.equals(id) && !"ROLE_ADMIN".equals(userRole)) {
            log.warn("PUT /users/{} forbidden - authenticated user {} is not owner and not ADMIN", id, authenticatedUserId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(service.updateUser(id, userRequestDto));
    }

    @Operation(summary = "Get all users", description = "Fetches all users.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users fetched successfully")
    })
    @GetMapping
    public ResponseEntity<?> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort,
            HttpServletRequest request) {
        if (!isPaginatedRequest(request)) {
        return ResponseEntity.ok(service.getAllUsers());
        }

        Pageable pageable = buildPageable(page, size, sort, "id", Sort.Direction.ASC);
        return ResponseEntity.ok(toPaginatedResponse(service.getAllUsers(pageable)));
    }

    @Operation(summary = "Get users by role", description = "Fetches users by their role. Supports optional keyword search by name or email.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users fetched successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid role provided"),
            @ApiResponse(responseCode = "403", description = "Forbidden — ADMIN role not allowed")
    })
    @GetMapping("/role/{role}")
    public ResponseEntity<?> getUsersByRole(
            @PathVariable String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {

        log.info("GET /users/role/{} - fetching users by role, keyword={}", role, keyword);

        try {
            String roleName = role.toUpperCase().replace("ROLE_", "");
            Role roleEnum = Role.valueOf(roleName);

            if (roleEnum == Role.ADMIN) {
                log.warn("Attempt to fetch ADMIN users - blocked");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Collections.emptyList());
            }

            // If keyword is provided, always use paginated keyword search
            if (keyword != null && !keyword.isBlank()) {
                Pageable pageable = buildPageable(page, size, sort, "id", Sort.Direction.ASC);
                Page<UserResponseDto> response = service.searchUsersByRoleAndKeyword(roleEnum, keyword.trim(), pageable);
                log.info("Keyword search returned {} users for role={}, keyword={}", response.getNumberOfElements(), role, keyword);
                return ResponseEntity.ok(toPaginatedResponse(response));
            }

            if (!isPaginatedRequest(request)) {
                List<UserResponseDto> response = service.getUsersByRole(roleEnum);
                log.info("Successfully fetched {} users with role: {}", response.size(), role);
                return ResponseEntity.ok(response);
            }

            Pageable pageable = buildPageable(page, size, sort, "id", Sort.Direction.ASC);
            Page<UserResponseDto> response = service.getUsersByRole(roleEnum, pageable);
            log.info("Successfully fetched {} users with role: {}", response.getNumberOfElements(), role);
            return ResponseEntity.ok(toPaginatedResponse(response));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid role provided: {}", role);
            return ResponseEntity.badRequest().body(Collections.emptyList());
        }
    }

    @Operation(summary = "Get public platform stats", description = "Fetches the public count of founders, investors, and co-founders.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stats fetched successfully")
    })
    @GetMapping("/public/stats")
    public ResponseEntity<Map<String, Long>> getPublicStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("founders", service.countByRole(Role.FOUNDER));
        stats.put("investors", service.countByRole(Role.INVESTOR));
        stats.put("cofounders", service.countByRole(Role.COFOUNDER));
        return ResponseEntity.ok(stats);
    }

    private boolean isValidInternalAccess(String authSource, String secret) {
        if (authSource == null || secret == null) {
            return false;
        }
        return EXPECTED_AUTH_SOURCE.equals(authSource) && internalSecret.equals(secret);
    }

    private boolean isPaginatedRequest(HttpServletRequest request) {
        return request.getParameterMap().containsKey("page");
    }

    private Pageable buildPageable(int page, int size, String sort, String defaultProperty, Sort.Direction defaultDirection) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Sort resolvedSort = resolveSort(sort, defaultProperty, defaultDirection);
        return PageRequest.of(safePage, safeSize, resolvedSort);
    }

    private Sort resolveSort(String sort, String defaultProperty, Sort.Direction defaultDirection) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(defaultDirection, defaultProperty);
        }

        String[] tokens = sort.split(",");
        String property = tokens[0].isBlank() ? defaultProperty : tokens[0].trim();
        Sort.Direction direction = tokens.length > 1 && "desc".equalsIgnoreCase(tokens[1].trim())
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direction, property);
    }

    private <T> Map<String, Object> toPaginatedResponse(Page<T> page) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("content", page.getContent());
        payload.put("page", page.getNumber());
        payload.put("size", page.getSize());
        payload.put("totalElements", page.getTotalElements());
        payload.put("totalPages", page.getTotalPages());
        payload.put("last", page.isLast());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", payload);
        response.put("error", null);
        return response;
    }
}
