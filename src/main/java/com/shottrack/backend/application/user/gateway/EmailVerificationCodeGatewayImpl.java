package com.shottrack.backend.application.user.gateway;

import com.shottrack.backend.application.user.gateway.repository.EmailVerificationCodeRepository;
import com.shottrack.backend.application.user.model.EmailVerificationCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class EmailVerificationCodeGatewayImpl implements EmailVerificationCodeGateway {

    private final EmailVerificationCodeRepository emailVerificationCodeRepository;

    @Override
    public EmailVerificationCode save(EmailVerificationCode code) {
        return emailVerificationCodeRepository.save(code);
    }

    @Override
    public Optional<EmailVerificationCode> findLatestPendingByUserId(UUID userId) {
        return emailVerificationCodeRepository.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(userId);
    }

    @Override
    public void invalidatePendingByUserId(UUID userId) {
        List<EmailVerificationCode> pending = emailVerificationCodeRepository.findAllByUserIdAndUsedFalse(userId);
        pending.forEach(EmailVerificationCode::markUsed);
        emailVerificationCodeRepository.saveAll(pending);
    }
}
