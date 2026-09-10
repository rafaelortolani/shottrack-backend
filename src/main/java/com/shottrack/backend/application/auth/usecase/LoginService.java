package com.shottrack.backend.application.auth.usecase;

import com.shottrack.backend.application.auth.dto.LoginRequest;
import com.shottrack.backend.application.auth.dto.TokenResponse;
import com.shottrack.backend.application.auth.gateway.RefreshTokenGateway;
import com.shottrack.backend.application.auth.model.RefreshToken;
import com.shottrack.backend.application.user.gateway.UserGateway;
import com.shottrack.backend.application.user.model.User;
import com.shottrack.backend.common.exception.BusinessException;
import com.shottrack.backend.common.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final UserGateway userGateway;
    private final RefreshTokenGateway refreshTokenGateway;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public TokenResponse login(LoginRequest request) {
        User user = userGateway.findByEmail(request.email())
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new BusinessException("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED));

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId());

        RefreshToken refreshToken = RefreshToken.builder()
                .token(jwtTokenProvider.generateRefreshTokenValue())
                .userId(user.getId())
                .expiresAt(jwtTokenProvider.refreshTokenExpiresAt())
                .build();
        refreshTokenGateway.save(refreshToken);

        return new TokenResponse(accessToken, refreshToken.getToken());
    }
}
