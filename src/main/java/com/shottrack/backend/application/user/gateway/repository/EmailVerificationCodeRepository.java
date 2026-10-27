package com.shottrack.backend.application.user.gateway.repository;

import com.shottrack.backend.application.user.model.EmailVerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, UUID> {

    Optional<EmailVerificationCode> findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(UUID userId);

    List<EmailVerificationCode> findAllByUserIdAndUsedFalse(UUID userId);

    void deleteAllByUserId(UUID userId);
    long deleteByExpiresAtBefore(Instant cutoff);
}
