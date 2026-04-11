package com.founderlink.User_Service.controller;

import com.founderlink.User_Service.dto.UpdateStatusRequest;
import com.founderlink.User_Service.dto.UserResponseDto;
import com.founderlink.User_Service.entity.Role;
import com.founderlink.User_Service.entity.User;
import com.founderlink.User_Service.entity.UserStatus;
import com.founderlink.User_Service.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

@Slf4j
@RestController
@RequestMapping("/users/admin")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final RestTemplate restTemplate;
    private final RestTemplate plainRestTemplate;

    @Operation(summary = "Update user status", description = "Updates a user's status to ACTIVE/SUSPENDED/BANNED")
    @Caching(evict = {
        @CacheEvict(value = "userById",    key = "#id"),
        @CacheEvict(value = "allUsers",    allEntries = true),
        @CacheEvict(value = "usersByRole", allEntries = true)
    })
    @PutMapping("/{id}/status")
    public ResponseEntity<UserResponseDto> updateStatus(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable Long id,
            @RequestBody UpdateStatusRequest request) {

        if (!"ROLE_ADMIN".equals(role)) {
            return ResponseEntity.status(403).build();
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new com.founderlink.User_Service.exceptions.UserNotFoundException("User not found"));

        user.setStatus(request.getStatus());
        user.setUpdatedAt(java.time.LocalDateTime.now());
        User saved = userRepository.save(user);

        // Sync to auth-service internally
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Secret", "trusted-internal-secret-xyz123");
            HttpEntity<?> entity = new HttpEntity<>(headers);
            restTemplate.exchange(
                    "http://auth-service/auth/admin/users/" + id + "/status?status=" + request.getStatus().name(),
                    HttpMethod.PUT,
                    entity,
                    Object.class
            );
            log.info("Successfully synced status {} for user {} to auth-service", request.getStatus(), id);
        } catch (Exception e) {
            log.error("Failed to sync status for user {} to auth-service: {}", id, e.getMessage());
            // Optionally, we could throw to rollback, but in this setup we log failure
        }

        return ResponseEntity.ok(modelMapper.map(saved, UserResponseDto.class));
    }

    @Operation(summary = "Search users", description = "Admin search across all users")
    @GetMapping("/search")
    public ResponseEntity<Page<UserResponseDto>> searchUsers(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Role searchRole,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
            
        if (!"ROLE_ADMIN".equals(role)) {
            return ResponseEntity.status(403).build();
        }

        Page<User> users = userRepository.searchUsers(
                email, name, searchRole, status,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"))
        );

        return ResponseEntity.ok(users.map(u -> modelMapper.map(u, UserResponseDto.class)));
    }

    @Operation(summary = "Get microservices health", description = "Query Prometheus for service health")
    @GetMapping("/health/microservices")
    public ResponseEntity<List<Map<String, Object>>> getMicroservicesHealth(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
            
        if (!"ROLE_ADMIN".equals(role)) {
            return ResponseEntity.status(403).build();
        }

        try {
            Map response = plainRestTemplate.getForObject("http://prometheus:9090/api/v1/query?query=up", Map.class);
            if (response != null && response.get("data") != null) {
                Map data = (Map) response.get("data");
                List<Map> results = (List<Map>) data.get("result");

                List<Map<String, Object>> healthList = new ArrayList<>();
                if (results != null) {
                    for (Map result : results) {
                        Map metric = (Map) result.get("metric");
                        List valueTuple = (List) result.get("value");
                        
                        String jobName = (String) metric.get("job");
                        String statusValue = (String) valueTuple.get(1);
                        
                        Map<String, Object> healthInfo = new HashMap<>();
                        healthInfo.put("name", jobName);
                        healthInfo.put("status", "1".equals(statusValue) ? "UP" : "DOWN");
                        healthList.add(healthInfo);
                    }
                }
                return ResponseEntity.ok(healthList);
            }
            return ResponseEntity.ok(new ArrayList<>());
        } catch (Exception e) {
            log.error("Prometheus monitoring unavailable: {}", e.getMessage());
            return ResponseEntity.ok(new ArrayList<>());
        }
    }
}
