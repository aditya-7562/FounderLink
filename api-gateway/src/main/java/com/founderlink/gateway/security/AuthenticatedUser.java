package com.founderlink.gateway.security;

public record AuthenticatedUser(String userId, Role role) {
}
