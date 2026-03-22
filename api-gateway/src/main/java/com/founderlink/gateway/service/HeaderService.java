package com.founderlink.gateway.service;

import com.founderlink.gateway.security.AuthenticatedUser;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;

@Service
public class HeaderService {

    private static final String USER_HEADER_PREFIX = "X-User-";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";
    private static final String AUTH_SOURCE_HEADER = "X-Auth-Source";
    private static final String ROLE_PREFIX = "ROLE_";


    public ServerWebExchange applyAuthenticationHeaders(ServerWebExchange exchange, AuthenticatedUser user) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    List<String> userHeaders = headers.keySet().stream()
                            .filter(name -> name.regionMatches(true, 0, USER_HEADER_PREFIX, 0, USER_HEADER_PREFIX.length()))
                            .toList();
                    userHeaders.forEach(headers::remove);
                    headers.remove(AUTH_SOURCE_HEADER);
                    headers.set(USER_ID_HEADER, user.userId());
                    headers.set(USER_ROLE_HEADER, ROLE_PREFIX + user.role().name());
                    headers.set(AUTH_SOURCE_HEADER, "gateway");
                })
                .build();

        return exchange.mutate().request(request).build();
    }
}
