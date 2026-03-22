package com.founderlink.gateway.service;

import com.founderlink.gateway.security.AuthenticatedUser;
import com.founderlink.gateway.security.Role;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

class HeaderServiceTest {

    private final HeaderService headerService = new HeaderService();

    @Test
    void appliesAuthenticationHeadersToDownstreamRequest() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/teams/invite")
                        .header("X-Correlation-Id", "corr-123")
                        .build()
        );

        ServerWebExchange mutatedExchange = headerService.applyAuthenticationHeaders(
                exchange,
                new AuthenticatedUser("user-42", Role.FOUNDER)
        );

        assertThat(mutatedExchange.getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("user-42");
        assertThat(mutatedExchange.getRequest().getHeaders().getFirst("X-User-Role")).isEqualTo("ROLE_FOUNDER");
        assertThat(mutatedExchange.getRequest().getHeaders().getFirst("X-Auth-Source")).isEqualTo("gateway");
        assertThat(mutatedExchange.getRequest().getHeaders().getFirst("X-Correlation-Id")).isEqualTo("corr-123");
    }

    @Test
    void removesSpoofedAuthenticationHeadersAndOverwritesThem() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/investments")
                        .header("X-User-Id", "spoofed-user")
                        .header("X-User-Role", "ADMIN")
                        .header("X-Auth-Source", "browser")
                        .header("X-User-Name", "spoofed-name")
                        .build()
        );

        ServerWebExchange mutatedExchange = headerService.applyAuthenticationHeaders(
                exchange,
                new AuthenticatedUser("investor-7", Role.INVESTOR)
        );

        assertThat(mutatedExchange.getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("investor-7");
        assertThat(mutatedExchange.getRequest().getHeaders().getFirst("X-User-Role")).isEqualTo("ROLE_INVESTOR");
        assertThat(mutatedExchange.getRequest().getHeaders().getFirst("X-Auth-Source")).isEqualTo("gateway");
        assertThat(mutatedExchange.getRequest().getHeaders().containsKey("X-User-Name")).isFalse();
    }
}
