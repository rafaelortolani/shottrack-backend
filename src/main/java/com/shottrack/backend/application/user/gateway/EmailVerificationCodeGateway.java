package com.shottrack.backend.application.user.gateway;

import com.shottrack.backend.application.user.model.EmailVerificationCode;

import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationCodeGateway {

    EmailVerificationCode save(EmailVerificationCode code);

    Optional<EmailVerificationCode> findLatestPendingByUserId(UUID userId);

    void invalidatePendingByUserId(UUID userId);
}
