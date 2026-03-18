package com.founderlink.auth.security;

public record JwtPrincipal(Long userId, String email, String role) {
}
