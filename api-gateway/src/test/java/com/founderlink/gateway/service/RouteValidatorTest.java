package com.founderlink.gateway.service;

import com.founderlink.gateway.config.GatewaySecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RouteValidatorTest {

    @Test
    void treatsConfiguredPublicPathAsUnsecuredForAnyMethod() {
        RouteValidator validator = new RouteValidator(properties(
                List.of("/auth/**"),
                List.of()
        ));

        assertThat(validator.isSecured(MockServerHttpRequest.post("/auth/login").build()))
                .isFalse();
    }

    @Test
    void treatsConfiguredPublicGetPathAsUnsecuredForGet() {
        RouteValidator validator = new RouteValidator(properties(
                List.of("/auth/**"),
                List.of("/startup", "/startup/search", "/startup/details/**")
        ));

        assertThat(validator.isSecured(MockServerHttpRequest.get("/startup/search").build()))
                .isFalse();
        assertThat(validator.isSecured(MockServerHttpRequest.get("/startup/details/42/").build()))
                .isFalse();
    }

    @Test
    void keepsNonGetMethodsSecuredForPublicGetPaths() {
        RouteValidator validator = new RouteValidator(properties(
                List.of("/auth/**"),
                List.of("/startup/search")
        ));

        assertThat(validator.isSecured(MockServerHttpRequest.post("/startup/search").build()))
                .isTrue();
    }

    @Test
    void treatsConfiguredPublicGetPathAsUnsecuredForHead() {
        RouteValidator validator = new RouteValidator(properties(
                List.of("/auth/**"),
                List.of("/startup/details/**")
        ));

        assertThat(validator.isSecured(MockServerHttpRequest.head("/startup/details/42").build()))
                .isFalse();
    }

    @Test
    void handlesNullConfiguredPathsGracefully() {
        RouteValidator validator = new RouteValidator(properties(null, null));
        
        // Everything should be secured by default when no public paths exist
        assertThat(validator.isSecured(MockServerHttpRequest.get("/auth/login").build()))
                .isTrue();
        assertThat(validator.isSecured(MockServerHttpRequest.post("/api/data").build()))
                .isTrue();
    }

    private GatewaySecurityProperties properties(List<String> publicPaths, List<String> publicGetPaths) {
        GatewaySecurityProperties properties = new GatewaySecurityProperties();
        properties.setPublicPaths(publicPaths);
        properties.setPublicGetPaths(publicGetPaths);
        return properties;
    }
}
