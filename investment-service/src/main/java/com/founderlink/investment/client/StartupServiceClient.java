package com.founderlink.investment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.founderlink.investment.dto.response.StartupResponseDto;

@FeignClient(
    name = "startup-service",
    fallback = StartupServiceClientFallback.class
)
public interface StartupServiceClient {

    @GetMapping("/startup/{id}")
    StartupResponseDto getStartupById(@PathVariable Long id);
}