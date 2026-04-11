package com.founderlink.gateway.filter;

import com.founderlink.gateway.security.AuthenticatedUser;
import com.founderlink.gateway.security.Role;
import com.founderlink.gateway.service.HeaderService;
import com.founderlink.gateway.service.JwtService;
import com.founderlink.gateway.service.RbacService;
import com.founderlink.gateway.service.RouteValidator;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthenticationFilterTest {

    private final RouteValidator routeValidator = mock(RouteValidator.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final RbacService rbacService = mock(RbacService.class);
    private final HeaderService headerService = mock(HeaderService.class);
    private final AuthenticationFilter filter = new AuthenticationFilter(routeValidator, jwtService, rbacService, headerService);

    @Test
    void returnsUnauthorizedWhenAuthorizationHeaderIsMissing() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/teams/invite").build()
        );
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(routeValidator.isSecured(exchange.getRequest())).thenReturn(true);

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any(ServerWebExchange.class));
        verify(jwtService, never()).authenticate(any());
    }

    @Test
    void returnsUnauthorizedWhenAuthorizationHeaderLacksBearerPrefix() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/teams/invite")
                    .header("Authorization", "Basic dXNlcjpwYXNz")
                    .build()
        );
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(routeValidator.isSecured(exchange.getRequest())).thenReturn(true);

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any(ServerWebExchange.class));
    }
    
    @Test
    void returnsUnauthorizedWhenBearerPrefixExistsButTokenIsEmpty() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/teams/invite")
                    .header("Authorization", "Bearer     ")
                    .build()
        );
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(routeValidator.isSecured(exchange.getRequest())).thenReturn(true);

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void returnsUnauthorizedWhenJwtServiceRejectsToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/teams/invite")
                        .header("Authorization", "Bearer expired-token")
                        .build()
        );
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(routeValidator.isSecured(exchange.getRequest())).thenReturn(true);
        when(jwtService.authenticate("expired-token"))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token"));

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any(ServerWebExchange.class));
        verify(rbacService, never()).verifyAccess(any(), any(), any());
    }

    @Test
    void returnsUnauthorizedWhenTokenDoesNotContainRole() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/investments/startup/42")
                        .header("Authorization", "Bearer missing-role")
                        .build()
        );
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(routeValidator.isSecured(exchange.getRequest())).thenReturn(true);
        when(jwtService.authenticate("missing-role"))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token role is missing"));

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any(ServerWebExchange.class));
        verify(rbacService, never()).verifyAccess(any(), any(), any());
    }

    @Test
    void returnsForbiddenWhenRbacRejectsRequest() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/teams/invite")
                        .header("Authorization", "Bearer investor-token")
                        .build()
        );
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        AuthenticatedUser user = new AuthenticatedUser("investor-9", Role.INVESTOR);

        when(routeValidator.isSecured(exchange.getRequest())).thenReturn(true);
        when(jwtService.authenticate("investor-token")).thenReturn(user);
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Role not allowed for request"))
                .when(rbacService).verifyAccess(exchange.getRequest().getMethod(), "/teams/invite", Role.INVESTOR);

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(chain, never()).filter(any(ServerWebExchange.class));
        verify(headerService, never()).applyAuthenticationHeaders(any(), any());
    }

    @Test
    void appliesHeadersAndContinuesWhenAuthenticationAndAuthorizationSucceed() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/investments/startup/42")
                        .header("Authorization", "Bearer founder-token")
                        .build()
        );
        MockServerWebExchange mutatedExchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/investments/startup/42")
                        .header("X-User-Id", "founder-1")
                        .header("X-User-Role", "ROLE_FOUNDER")
                        .header("X-Auth-Source", "gateway")
                        .build()
        );
        AuthenticatedUser user = new AuthenticatedUser("founder-1", Role.FOUNDER);
        AtomicReference<ServerWebExchange> chainedExchange = new AtomicReference<>();
        GatewayFilterChain chain = chained -> {
            chainedExchange.set(chained);
            return Mono.empty();
        };

        when(routeValidator.isSecured(exchange.getRequest())).thenReturn(true);
        when(jwtService.authenticate("founder-token")).thenReturn(user);
        when(headerService.applyAuthenticationHeaders(exchange, user)).thenReturn(mutatedExchange);

        filter.filter(exchange, chain).block();

        verify(rbacService).verifyAccess(exchange.getRequest().getMethod(), "/investments/startup/42", Role.FOUNDER);
        verify(headerService).applyAuthenticationHeaders(exchange, user);
        assertThat(chainedExchange.get()).isSameAs(mutatedExchange);
    }

    @Test
    void bypassesAuthenticationForPublicRoute() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/auth/login").build()
        );
        AtomicReference<ServerWebExchange> chainedExchange = new AtomicReference<>();
        GatewayFilterChain chain = chained -> {
            chainedExchange.set(chained);
            return Mono.empty();
        };

        when(routeValidator.isSecured(exchange.getRequest())).thenReturn(false);

        filter.filter(exchange, chain).block();

        assertThat(chainedExchange.get()).isSameAs(exchange);
        verifyNoSecurityInteractions();
    }

    private void verifyNoSecurityInteractions() {
        verify(jwtService, never()).authenticate(any());
        verify(rbacService, never()).verifyAccess(any(), any(), any());
        verify(headerService, never()).applyAuthenticationHeaders(any(), any());
        Mockito.verifyNoMoreInteractions(jwtService, rbacService, headerService);
    }

    @Test
    void bypassesAuthenticationForOptionsMethod() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.options("/api/something").build()
        );
        AtomicReference<ServerWebExchange> chainedExchange = new AtomicReference<>();
        GatewayFilterChain chain = chained -> {
            chainedExchange.set(chained);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(chainedExchange.get()).isSameAs(exchange);
        verifyNoSecurityInteractions();
    }

    @Test
    void catchesGenericExceptionAndReturnsInternalServerError() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/fail/endpoint")
                        .header("Authorization", "Bearer normal-token")
                        .build()
        );
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        
        when(routeValidator.isSecured(exchange.getRequest())).thenReturn(true);
        // Force a runtime exception from inside the filter logic
        when(jwtService.authenticate("normal-token")).thenThrow(new RuntimeException("Unexpected systemic crash"));

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        verify(chain, never()).filter(any(ServerWebExchange.class));
    }

    @Test
    void debuggingTokenDecodeHandlesVariousLengthsSafely() {
        // Trigger the internal try-catch of decoding parts by sending a token with missing parts
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/teams/invite")
                        .header("Authorization", "Bearer invalid.short")
                        .build()
        );
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(routeValidator.isSecured(exchange.getRequest())).thenReturn(true);
        when(jwtService.authenticate("invalid.short"))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token"));

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
