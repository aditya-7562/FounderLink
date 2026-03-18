package com.founderlink.auth.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Component
@Validated
@Getter
@Setter
@ConfigurationProperties(prefix = "security.refresh-token")
public class RefreshTokenProperties {

    @NotNull
    private Duration expiration = Duration.ofDays(30);

    @NotBlank
    private String cookieName = "refresh_token";

    @NotBlank
    private String cookiePath = "/auth";

    @NotBlank
    private String cookieSameSite = "Strict";

    private boolean cookieSecure = true;
}
