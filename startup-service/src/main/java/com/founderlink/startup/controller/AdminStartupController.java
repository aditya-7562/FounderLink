package com.founderlink.startup.controller;

import com.founderlink.startup.dto.UpdateModerationRequest;
import com.founderlink.startup.dto.response.ApiResponse;
import com.founderlink.startup.entity.Startup;
import com.founderlink.startup.repository.StartupRepository;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/startup/admin")
@RequiredArgsConstructor
public class AdminStartupController {

    private final StartupRepository startupRepository;

    @Operation(summary = "Update startup moderation status", description = "Approve, reject, or flag a startup.")
    @Caching(evict = {
        @CacheEvict(value = "startupCache_byId", key = "#id"),
        @CacheEvict(value = "startupCache_active", allEntries = true)
    })
    @PutMapping("/{id}/moderation")
    public ResponseEntity<ApiResponse<?>> updateModerationStatus(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable Long id,
            @Valid @RequestBody UpdateModerationRequest request) {

        if (!"ROLE_ADMIN".equals(role)) {
            return ResponseEntity.status(403).build();
        }

        Startup startup = startupRepository.findById(id)
                .orElseThrow(() -> new com.founderlink.startup.exception.StartupNotFoundException("Startup not found with id: " + id));

        startup.setModerationStatus(request.getStatus());
        startup.setModerationReason(request.getReason());
        startupRepository.save(startup);

        log.info("Startup {} moderation status updated to {} by ADMIN", id, request.getStatus());

        return ResponseEntity.ok(new ApiResponse<>("Moderation status updated", null));
    }

    @Operation(summary = "Force delete startup", description = "Permanently or soft deletes a startup bypassing founder ownership")
    @Caching(evict = {
        @CacheEvict(value = "startupCache_byId", key = "#id"),
        @CacheEvict(value = "startupCache_active", allEntries = true)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> deleteStartup(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable Long id) {

        if (!"ROLE_ADMIN".equals(role)) {
            return ResponseEntity.status(403).build();
        }

        Startup startup = startupRepository.findById(id)
                .orElseThrow(() -> new com.founderlink.startup.exception.StartupNotFoundException("Startup not found with id: " + id));

        // Soft delete
        startup.setIsDeleted(true);
        startupRepository.save(startup);

        log.info("Startup {} force deleted by ADMIN", id);

        return ResponseEntity.ok(new ApiResponse<>("Startup successfully deleted", null));
    }
}
