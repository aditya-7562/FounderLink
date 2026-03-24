package com.founderlink.gateway.filter;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import com.founderlink.gateway.security.AuthenticatedUser;
import com.founderlink.gateway.service.HeaderService;
import com.founderlink.gateway.service.JwtService;
import com.founderlink.gateway.service.RbacService;
import com.founderlink.gateway.service.RouteValidator;

import reactor.core.publisher.Mono;

@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);

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
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";
        
        log.info("=== AUTHENTICATION DEBUG ===");
        log.info("Request: {} {}", method, path);
        log.info("Request Headers: {}", request.getHeaders());
        
        // Check if endpoint is public
        boolean isSecured = routeValidator.isSecured(request);
        log.info("Is endpoint secured? {}", isSecured);
        
        if (!isSecured) {
            log.info("Public endpoint accessed - bypassing authentication");
            return chain.filter(exchange);
        }
        
        log.info("Processing secured endpoint");

        try {
            // Extract token
            String token = extractBearerToken(request);
            log.info("Token extracted successfully. Token length: {}", token.length());
            log.debug("Token (first 50 chars): {}", token.substring(0, Math.min(50, token.length())));
            
            // Decode token payload for debugging
            try {
                String[] parts = token.split("\\.");
                if (parts.length == 3) {
                    String header = new String(Base64.getUrlDecoder().decode(parts[0]));
                    String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
                    log.debug("JWT Header: {}", header);
                    log.debug("JWT Payload: {}", payload);
                } else {
                    log.warn("Token does not have 3 parts. Parts count: {}", parts.length);
                }
            } catch (Exception e) {
                log.debug("Could not decode token: {}", e.getMessage());
            }
            
            // Authenticate user
            AuthenticatedUser user = jwtService.authenticate(token);
            log.info("Authentication successful - UserId: {}, Role: {}", user.userId(), user.role());
            
            // Verify RBAC
            log.info("Checking RBAC access for method={}, path={}, role={}", method, path, user.role());
            rbacService.verifyAccess(request.getMethod(), path, user.role());
            log.info("RBAC verification passed");
            
            // Add authentication headers
            ServerWebExchange authenticatedExchange = 
                    headerService.applyAuthenticationHeaders(exchange, user);
            log.info("Added authentication headers: X-User-Id={}, X-User-Role={}", 
                user.userId(), "ROLE_" + user.role().name());
            
            log.info("=== REQUEST PROCESSED SUCCESSFULLY ===");
            return chain.filter(authenticatedExchange);
            
        } catch (ResponseStatusException ex) {
            log.error("Authentication/RBAC failed: {} - {}", ex.getStatusCode(), ex.getReason());
            log.error("Full exception", ex);
            
            exchange.getResponse().setStatusCode(ex.getStatusCode());
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            
            // Create detailed error response
            String errorBody = String.format(
                "{\"timestamp\": \"%s\", \"status\": %d, \"error\": \"%s\", \"message\": \"%s\", \"path\": \"%s\", \"method\": \"%s\"}",
                java.time.Instant.now(),
                ex.getStatusCode().value(),
                ((HttpStatus) ex.getStatusCode()).getReasonPhrase(),
                ex.getReason(),
                path,
                method
            );
            
            byte[] bytes = errorBody.getBytes(StandardCharsets.UTF_8);
            return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(bytes))
            );
            
        } catch (Exception ex) {
            log.error("Unexpected error during authentication", ex);
            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            
            String errorBody = String.format(
                "{\"timestamp\": \"%s\", \"status\": 500, \"error\": \"Internal Server Error\", \"message\": \"%s\", \"path\": \"%s\"}",
                java.time.Instant.now(),
                ex.getMessage(),
                path
            );
            
            byte[] bytes = errorBody.getBytes(StandardCharsets.UTF_8);
            return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(bytes))
            );
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private String extractBearerToken(ServerHttpRequest request) {
        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        log.debug("Authorization header: {}", authorization);
        
        if (!StringUtils.hasText(authorization)) {
            log.warn("Authorization header is missing");
            throw unauthorized("Missing Authorization header");
        }
        
        if (!authorization.startsWith("Bearer ")) {
            log.warn("Authorization header does not start with 'Bearer '. Actual: {}", 
                authorization.substring(0, Math.min(20, authorization.length())));
            throw unauthorized("Authorization header must start with 'Bearer '");
        }

        String token = authorization.substring(7).trim();
        if (!StringUtils.hasText(token)) {
            log.warn("Bearer token is empty after Bearer prefix");
            throw unauthorized("Missing bearer token");
        }
        
        return token;
    }

    private ResponseStatusException unauthorized(String message) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }
}