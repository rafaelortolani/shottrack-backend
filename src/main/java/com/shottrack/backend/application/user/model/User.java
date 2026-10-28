package com.shottrack.backend.application.user.model;

import com.shottrack.backend.common.jpa.AbstractBaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@Entity
@Table(name = "users")
public class User extends AbstractBaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Setter
    @NonNull
    @Column(nullable = false)
    private String name;

    @Setter
    @NonNull
    @Column(nullable = false, unique = true)
    private String email;

    @Setter
    @NonNull
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "experience_level", nullable = false)
    private ExperienceLevel experienceLevel = ExperienceLevel.BEGINNER;

    /**
     * ADR-0021: fora do builder e sem setter — só muda por UPDATE direto no banco.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Column(name = "profile_completed_at")
    private Instant profileCompletedAt;

    @Column(name = "terms_accepted_at")
    private Instant termsAcceptedAt;

    @Column(name = "terms_version")
    private String termsVersion;

    @Builder
    private User(@NonNull String name, @NonNull String email, @NonNull String passwordHash,
                 ExperienceLevel experienceLevel, Instant termsAcceptedAt, String termsVersion) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.termsAcceptedAt = termsAcceptedAt;
        this.termsVersion = termsVersion;
        if (experienceLevel != null) {
            this.experienceLevel = experienceLevel;
        }
    }

    /**
     * UC42 (onboarding): fora do builder — só muda por aqui, na primeira
     * edição de perfil (UC04). Edições seguintes mantêm a data original.
     */
    public void markProfileCompleted(Instant completedAt) {
        if (profileCompletedAt == null) {
            this.profileCompletedAt = completedAt;
        }
    }

    public boolean isProfileCompleted() {
        return profileCompletedAt != null;
    }
}
