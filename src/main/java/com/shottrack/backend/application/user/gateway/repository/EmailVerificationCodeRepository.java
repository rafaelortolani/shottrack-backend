package com.shottrack.backend.application.user.gateway.repository;

import com.shottrack.backend.application.user.model.EmailVerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, UUID> {

    Optional<EmailVerificationCode> findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(UUID userId);

    List<EmailVerificationCode> findAllByUserIdAndUsedFalse(UUID userId);
}
