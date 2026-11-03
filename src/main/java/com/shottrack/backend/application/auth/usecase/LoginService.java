package com.shottrack.backend.application.auth.usecase;

import com.shottrack.backend.application.auth.dto.LoginRequest;
import com.shottrack.backend.application.auth.dto.TokenResponse;
import com.shottrack.backend.application.auth.gateway.RefreshTokenGateway;
import com.shottrack.backend.application.auth.model.RefreshToken;
import com.shottrack.backend.application.user.gateway.UserGateway;
import com.shottrack.backend.application.user.model.User;
import com.shottrack.backend.common.exception.BusinessException;
import com.shottrack.backend.common.security.JwtTokenProvider;
import com.shottrack.backend.common.security.TokenHasher;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final UserGateway userGateway;
    private final RefreshTokenGateway refreshTokenGateway;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * S05: hash BCrypt válido (mesmo custo dos reais) de uma senha que ninguém conhece — comparado quando o e-mail
     * não existe, pra resposta de "e-mail inexistente" levar o mesmo tempo da de "senha errada".
     */
    private String unknownUserHash;

    @PostConstruct
    void prepareUnknownUserHash() {
        unknownUserHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public TokenResponse login(LoginRequest request) {
        Optional<User> found = userGateway.findByEmail(request.email());
        if (found.isEmpty()) {
            passwordEncoder.matches(request.password(), unknownUserHash);
        }
        User user = found
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new BusinessException("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED));

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId());

        String refreshTokenValue = jwtTokenProvider.generateRefreshTokenValue();
        RefreshToken refreshToken = RefreshToken.builder()
                .tokenHash(TokenHasher.sha256Hex(refreshTokenValue))
                .userId(user.getId())
                .expiresAt(jwtTokenProvider.refreshTokenExpiresAt())
                .build();
        refreshTokenGateway.save(refreshToken);

        return new TokenResponse(accessToken, refreshTokenValue);
    }
}
