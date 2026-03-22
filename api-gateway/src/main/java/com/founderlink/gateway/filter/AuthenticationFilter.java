package com.founderlink.gateway.filter;

import com.founderlink.gateway.security.AuthenticatedUser;
import com.founderlink.gateway.service.HeaderService;
import com.founderlink.gateway.service.JwtService;
import com.founderlink.gateway.service.RbacService;
import com.founderlink.gateway.service.RouteValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private final RouteValidator routeValidator;
    private final JwtService jwtService;
    private final RbacService rbacService;
    private final HeaderService headerService;

    @Autowired
    public AuthenticationFilter(
            RouteValidator routeValidator,
            JwtService jwtService,
            RbacService rbacService,
            HeaderService headerService
    ) {
        this.routeValidator = routeValidator;
        this.jwtService = jwtService;
        this.rbacService = rbacService;
        this.headerService = headerService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!routeValidator.isSecured(exchange.getRequest())) {
            return chain.filter(exchange);
        }

        try {
            String token = extractBearerToken(exchange.getRequest());
            AuthenticatedUser user = jwtService.authenticate(token);
            rbacService.verifyAccess(
                    exchange.getRequest().getMethod(),
                    exchange.getRequest().getPath().value(),
                    user.role()
            );

            ServerWebExchange authenticatedExchange =
                    headerService.applyAuthenticationHeaders(exchange, user);
            return chain.filter(authenticatedExchange);
        } catch (ResponseStatusException ex) {
            exchange.getResponse().setStatusCode(ex.getStatusCode());
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private String extractBearerToken(ServerHttpRequest request) {
        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            throw unauthorized("Missing Authorization header");
        }

        String token = authorization.substring(7).trim();
        if (!StringUtils.hasText(token)) {
            throw unauthorized("Missing bearer token");
        }

        return token;
    }

    private ResponseStatusException unauthorized(String message) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }
}
