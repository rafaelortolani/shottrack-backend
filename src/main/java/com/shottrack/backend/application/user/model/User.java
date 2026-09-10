package com.shottrack.backend.application.user.model;

import com.shottrack.backend.common.jpa.AbstractBaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

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

    @Builder
    private User(@NonNull String name, @NonNull String email, @NonNull String passwordHash, ExperienceLevel experienceLevel) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        if (experienceLevel != null) {
            this.experienceLevel = experienceLevel;
        }
    }
}
