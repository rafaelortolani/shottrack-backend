package com.shottrack.backend.application.auth.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {
}
