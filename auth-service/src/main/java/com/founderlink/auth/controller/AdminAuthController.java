package com.founderlink.auth.controller;

import com.founderlink.auth.entity.User;
import com.founderlink.auth.entity.UserStatus;
import com.founderlink.auth.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final UserRepository userRepository;

    @Operation(summary = "Update user status (Internal / Admin)", description = "Updates a user's status for login guard enforcement.")
    @PutMapping("/users/{id}/status")
    public ResponseEntity<?> updateUserStatus(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-Internal-Secret", required = false) String internalSecret,
            @PathVariable Long id,
            @RequestParam UserStatus status) {

        if (!"ROLE_ADMIN".equals(role) && !"trusted-internal-secret-xyz123".equals(internalSecret)) {
            return ResponseEntity.status(403).build();
        }

        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        user.setStatus(status);
        userRepository.save(user);
        log.info("Auth service user {} status updated to {}", id, status);

        return ResponseEntity.ok(Map.of("message", "Status updated successfully"));
    }
}
