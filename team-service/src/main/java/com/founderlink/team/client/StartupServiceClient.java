package com.founderlink.team.client;

import com.founderlink.team.dto.response.StartupResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "startup-service",
    fallback = StartupServiceClientFallback.class
)
public interface StartupServiceClient {

    @GetMapping("/startup/{id}")
    StartupResponseDto getStartupById(@PathVariable Long id);
}