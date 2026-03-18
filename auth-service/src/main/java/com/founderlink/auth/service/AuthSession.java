package com.founderlink.auth.service;

import com.founderlink.auth.dto.AuthResponse;

public record AuthSession(AuthResponse authResponse, String refreshToken) {
}
