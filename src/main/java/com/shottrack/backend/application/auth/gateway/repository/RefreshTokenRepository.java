package com.shottrack.backend.application.auth.gateway.repository;

import com.shottrack.backend.application.auth.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findAllByUserIdAndRevokedFalse(UUID userId);

    void deleteAllByUserId(UUID userId);
    long deleteByExpiresAtBefore(Instant cutoff);
}
