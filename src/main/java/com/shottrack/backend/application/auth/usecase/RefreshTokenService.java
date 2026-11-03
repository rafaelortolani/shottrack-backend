package com.shottrack.backend.application.auth.usecase;

import com.shottrack.backend.application.auth.dto.LogoutRequest;
import com.shottrack.backend.application.auth.dto.RefreshRequest;
import com.shottrack.backend.application.auth.dto.TokenResponse;
import com.shottrack.backend.application.auth.gateway.RefreshTokenGateway;
import com.shottrack.backend.application.auth.model.RefreshToken;
import com.shottrack.backend.common.exception.BusinessException;
import com.shottrack.backend.common.security.JwtTokenProvider;
import com.shottrack.backend.common.security.TokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * ADR-0001: rotação a cada uso. Um refresh token só serve pra ser trocado por um
 * novo par (access + refresh) uma única vez; se ele reaparecer depois de já usado,
 * é tratado como sinal de roubo e toda a sessão do usuário é derrubada.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenGateway refreshTokenGateway;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * UC51/ADR-0018: idempotente — token desconhecido, já revogado ou expirado
     * também é sucesso. Um token já revogado aqui NÃO dispara a derrubada de
     * todas as sessões (isso é só do refresh, onde o reuso indica roubo).
     */
    public void logout(LogoutRequest request) {
        refreshTokenGateway.findByTokenHash(TokenHasher.sha256Hex(request.refreshToken()))
                .filter(stored -> !stored.isRevoked())
                .ifPresent(stored -> {
                    stored.revoke();
                    refreshTokenGateway.save(stored);
                });
    }

    public TokenResponse refresh(RefreshRequest request) {
        RefreshToken stored = refreshTokenGateway.findByTokenHash(TokenHasher.sha256Hex(request.refreshToken()))
                .orElseThrow(() -> new BusinessException("INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED));

        if (stored.isRevoked()) {
            refreshTokenGateway.revokeAllByUserId(stored.getUserId());
            throw new BusinessException("INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED);
        }

        if (stored.isExpired()) {
            throw new BusinessException("INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED);
        }

        // S20: a revogação é um UPDATE condicional (WHERE revoked = false). Duas chamadas simultâneas com o
        // mesmo token passam pelas checagens acima, mas só uma muda a linha; a que perde é reuso do token
        // (ADR-0001) e derruba as sessões do usuário.
        if (!refreshTokenGateway.revokeIfActive(stored.getId())) {
            refreshTokenGateway.revokeAllByUserId(stored.getUserId());
            throw new BusinessException("INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED);
        }

        String accessToken = jwtTokenProvider.generateAccessToken(stored.getUserId());
        String newRefreshTokenValue = jwtTokenProvider.generateRefreshTokenValue();
        RefreshToken newRefreshToken = RefreshToken.builder()
                .tokenHash(TokenHasher.sha256Hex(newRefreshTokenValue))
                .userId(stored.getUserId())
                .expiresAt(jwtTokenProvider.refreshTokenExpiresAt())
                .build();
        refreshTokenGateway.save(newRefreshToken);

        return new TokenResponse(accessToken, newRefreshTokenValue);
    }
}
