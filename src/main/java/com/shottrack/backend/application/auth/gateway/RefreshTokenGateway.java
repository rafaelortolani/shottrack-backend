package com.shottrack.backend.application.auth.gateway;

import com.shottrack.backend.application.auth.model.RefreshToken;

import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

public interface RefreshTokenGateway {

    RefreshToken save(RefreshToken refreshToken);

    Optional<RefreshToken> findByToken(String token);

    void revokeAllByUserId(UUID userId);

    void deleteAllByUserId(UUID userId);
    long deleteByExpiresAtBefore(Instant cutoff);
}
