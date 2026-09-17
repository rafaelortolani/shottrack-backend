package com.shottrack.backend.application.user.model;

import com.shottrack.backend.common.jpa.AbstractBaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Cadastro pendente (UC01/ADR-0009) — vira um User de verdade só quando o
 * token é confirmado em UC23, antes disso o email ainda não foi comprovado.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@Entity
@Table(name = "pending_registrations")
public class PendingRegistration extends AbstractBaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private final String email;

    @Column(nullable = false, unique = true)
    private final String token;

    @Column(name = "expires_at", nullable = false)
    private final Instant expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    @Builder
    private PendingRegistration(@NonNull String email, @NonNull String token, @NonNull Instant expiresAt) {
        this.email = email;
        this.token = token;
        this.expiresAt = expiresAt;
    }

    public void markUsed() {
        this.used = true;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
