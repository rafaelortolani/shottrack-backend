package com.shottrack.backend.common.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * Gera o access token (JWT assinado, claim `sub` = id do usuário) e o valor do
 * refresh token (string opaca aleatória — não é JWT, só é validado contra o que
 * foi persistido em RefreshToken, conforme ADR-0001).
 */
@Component
public class JwtTokenProvider {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SecretKey signingKey;
    private final Duration accessTokenExpiration;
    private final Duration refreshTokenExpiration;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiration-minutes}") long accessTokenExpirationMinutes,
            @Value("${app.jwt.refresh-token-expiration-days}") long refreshTokenExpirationDays) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessTokenExpiration = Duration.ofMinutes(accessTokenExpirationMinutes);
        this.refreshTokenExpiration = Duration.ofDays(refreshTokenExpirationDays);
    }

    public String generateAccessToken(UUID userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenExpiration)))
                .signWith(signingKey)
                .compact();
    }

    public String generateRefreshTokenValue() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public Instant refreshTokenExpiresAt() {
        return Instant.now().plus(refreshTokenExpiration);
    }
}
