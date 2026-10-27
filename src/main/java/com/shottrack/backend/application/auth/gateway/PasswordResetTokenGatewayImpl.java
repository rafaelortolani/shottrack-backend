package com.shottrack.backend.application.auth.gateway;

import com.shottrack.backend.application.auth.gateway.repository.PasswordResetTokenRepository;
import com.shottrack.backend.application.auth.model.PasswordResetToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class PasswordResetTokenGatewayImpl implements PasswordResetTokenGateway {

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        return passwordResetTokenRepository.save(token);
    }

    @Override
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        return passwordResetTokenRepository.findByTokenHash(tokenHash);
    }

    @Override
    public void deletePendingByUserId(UUID userId) {
        passwordResetTokenRepository.deleteAll(passwordResetTokenRepository.findAllByUserIdAndUsedAtIsNull(userId));
    }
}
