package com.shottrack.backend.application.auth.model;

import com.shottrack.backend.common.jpa.AbstractBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Token de redefinição de senha (UC49/UC50/ADR-0018) — guarda só o hash do
 * token enviado por e-mail. Mesmo padrão de uso único do cadastro (ADR-0009).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken extends AbstractBaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private final UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true)
    private final String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private final Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Builder
    private PasswordResetToken(@NonNull UUID userId, @NonNull String tokenHash, @NonNull Instant expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public void markUsed(Instant when) {
        this.usedAt = when;
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
