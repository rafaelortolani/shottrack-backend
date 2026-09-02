package com.shottrack.backend.application.auth.usecase;

import com.shottrack.backend.application.auth.dto.RefreshRequest;
import com.shottrack.backend.application.auth.dto.TokenResponse;
import com.shottrack.backend.application.auth.gateway.RefreshTokenGateway;
import com.shottrack.backend.application.auth.model.RefreshToken;
import com.shottrack.backend.common.exception.BusinessException;
import com.shottrack.backend.common.security.JwtTokenProvider;
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

    public TokenResponse refresh(RefreshRequest request) {
        RefreshToken stored = refreshTokenGateway.findByToken(request.refreshToken())
                .orElseThrow(() -> new BusinessException("INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED));

        if (stored.isRevoked()) {
            refreshTokenGateway.revokeAllByUserId(stored.getUserId());
            throw new BusinessException("INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED);
        }

        if (stored.isExpired()) {
            throw new BusinessException("INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED);
        }

        stored.revoke();
        refreshTokenGateway.save(stored);

        String accessToken = jwtTokenProvider.generateAccessToken(stored.getUserId());
        RefreshToken newRefreshToken = new RefreshToken(
                jwtTokenProvider.generateRefreshTokenValue(),
                stored.getUserId(),
                jwtTokenProvider.refreshTokenExpiresAt()
        );
        refreshTokenGateway.save(newRefreshToken);

        return new TokenResponse(accessToken, newRefreshToken.getToken());
    }
}
