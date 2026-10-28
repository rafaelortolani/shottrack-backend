package com.shottrack.backend.application.auth.gateway;

import com.shottrack.backend.application.auth.model.PasswordResetToken;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenGateway {

    PasswordResetToken save(PasswordResetToken token);

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    void deletePendingByUserId(UUID userId);
}
