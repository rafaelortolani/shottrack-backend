package com.shottrack.backend.application.user.model;

import com.shottrack.backend.common.jpa.AbstractBaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Código de confirmação da troca de email (ADR-0002). O email só é efetivamente
 * trocado em User quando o código correto é confirmado antes de expires_at.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@Entity
@Table(name = "email_verification_codes")
public class EmailVerificationCode extends AbstractBaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private final UUID userId;

    @Column(name = "new_email", nullable = false)
    private final String newEmail;

    @Column(nullable = false, length = 6)
    private final String code;

    @Column(name = "expires_at", nullable = false)
    private final Instant expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    @Builder
    private EmailVerificationCode(UUID userId, String newEmail, String code, Instant expiresAt) {
        this.userId = userId;
        this.newEmail = newEmail;
        this.code = code;
        this.expiresAt = expiresAt;
    }

    public void markUsed() {
        this.used = true;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
