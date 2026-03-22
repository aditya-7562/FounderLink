package com.founderlink.notification.client;

import com.founderlink.notification.dto.StartupDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "startup-service", fallback = StartupServiceClientFallback.class)
public interface StartupServiceClient {

    @GetMapping("/startup/{id}")
    StartupDTO getStartupById(@PathVariable Long id);
}
