package com.founderlink.payment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.founderlink.payment.dto.external.StartupResponseDto;

/**
 * Feign client for startup-service.
 * Communicates with startup-service to validate startup details.
 */
@FeignClient(name = "startup-service")
public interface StartupServiceClient {

    /**
     * Get startup details by ID.
     * GET /startup/{startupId}
     */
    @GetMapping("/startup/{startupId}")
    StartupResponseDto getStartupById(@PathVariable Long startupId);
}
