package com.shottrack.backend.application.user.gateway;

import com.shottrack.backend.application.user.model.EmailVerificationCode;

import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

public interface EmailVerificationCodeGateway {

    EmailVerificationCode save(EmailVerificationCode code);

    Optional<EmailVerificationCode> findLatestPendingByUserId(UUID userId);

    void invalidatePendingByUserId(UUID userId);

    void deleteAllByUserId(UUID userId);
    long deleteByExpiresAtBefore(Instant cutoff);
}
