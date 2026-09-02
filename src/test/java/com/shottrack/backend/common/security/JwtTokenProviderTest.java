package com.shottrack.backend.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class JwtTokenProviderTest {

    private static final String SECRET = "bOpQr/Lw9oOIih9aKiUKlxWHfKGgO5AhbW90gT/v7bw=";

    private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(SECRET, 60, 7);

    @Test
    void deveGerarAccessTokenComSubjectEExpiracaoDeUmaHora() {
        UUID userId = UUID.randomUUID();
        Instant before = Instant.now();

        String accessToken = jwtTokenProvider.generateAccessToken(userId);

        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(accessToken)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.getExpiration().toInstant())
                .isCloseTo(before.plusSeconds(3600), within(5, ChronoUnit.SECONDS));
    }

    @Test
    void deveGerarRefreshTokensDiferentesACadaChamada() {
        String first = jwtTokenProvider.generateRefreshTokenValue();
        String second = jwtTokenProvider.generateRefreshTokenValue();

        assertThat(first).isNotBlank();
        assertThat(second).isNotBlank();
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void deveExpirarRefreshTokenEmSeteDias() {
        Instant before = Instant.now();

        Instant expiresAt = jwtTokenProvider.refreshTokenExpiresAt();

        assertThat(expiresAt).isCloseTo(before.plusSeconds(7 * 24 * 3600), within(5, ChronoUnit.SECONDS));
    }
}
