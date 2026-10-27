package com.shottrack.backend.application.user.usecase;

import com.shottrack.backend.application.auth.gateway.PasswordResetTokenGateway;
import com.shottrack.backend.application.auth.gateway.RefreshTokenGateway;
import com.shottrack.backend.application.user.gateway.EmailVerificationCodeGateway;
import com.shottrack.backend.application.user.gateway.PendingRegistrationGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * ADR-0019: limpeza de dados vencidos (cadastros pendentes, tokens de
 * redefinição, refresh tokens e códigos de verificação). Só apaga o que venceu
 * há mais de 7 dias: a margem preserva refresh tokens revogados/expirados pra
 * depuração e mantém "expirado" (e não "inexistente") como resposta pra links
 * velhos, no dia seguinte ao vencimento.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpiredDataCleanupService {

    static final Duration RETENTION_AFTER_EXPIRY = Duration.ofDays(7);

    private final PendingRegistrationGateway pendingRegistrationGateway;
    private final PasswordResetTokenGateway passwordResetTokenGateway;
    private final RefreshTokenGateway refreshTokenGateway;
    private final EmailVerificationCodeGateway emailVerificationCodeGateway;

    @Transactional
    public void cleanup() {
        Instant cutoff = Instant.now().minus(RETENTION_AFTER_EXPIRY);

        long pendingRegistrations = pendingRegistrationGateway.deleteByExpiresAtBefore(cutoff);
        long passwordResetTokens = passwordResetTokenGateway.deleteByExpiresAtBefore(cutoff);
        long refreshTokens = refreshTokenGateway.deleteByExpiresAtBefore(cutoff);
        long verificationCodes = emailVerificationCodeGateway.deleteByExpiresAtBefore(cutoff);

        log.info("Limpeza de dados vencidos: cadastros pendentes={}, tokens de redefinição={}, refresh tokens={}, códigos de verificação={}",
                pendingRegistrations, passwordResetTokens, refreshTokens, verificationCodes);
    }
}
