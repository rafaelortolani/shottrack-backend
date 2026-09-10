package com.shottrack.backend.application.auth.model;

import com.shottrack.backend.common.jpa.AbstractBaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken extends AbstractBaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private final String token;

    @Column(name = "user_id", nullable = false)
    private final UUID userId;

    @Column(name = "expires_at", nullable = false)
    private final Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    @Builder
    private RefreshToken(String token, UUID userId, Instant expiresAt) {
        this.token = token;
        this.userId = userId;
        this.expiresAt = expiresAt;
    }

    /**
     * Marca o token como usado/revogado (ADR-0001: rotação a cada uso — o token
     * trocado por um novo access token nunca mais pode ser aceito de novo).
     */
    public void revoke() {
        this.revoked = true;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
