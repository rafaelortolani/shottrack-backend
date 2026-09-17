package com.shottrack.backend.application.traininglocation.model;

import com.shottrack.backend.common.jpa.AbstractBaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

import java.util.UUID;

/**
 * Cadastro livre (ADR-0010) — sem catálogo de clubes parceiros ainda.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@Entity
@Table(name = "training_locations")
public class TrainingLocation extends AbstractBaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private final UUID userId;

    @Setter
    @NonNull
    @Column(nullable = false)
    private String name;

    @Setter
    @NonNull
    @Column(nullable = false)
    private String city;

    @Setter
    @NonNull
    @Column(nullable = false)
    private String state;

    @Builder
    private TrainingLocation(UUID userId, @NonNull String name, @NonNull String city, @NonNull String state) {
        this.userId = userId;
        this.name = name;
        this.city = city;
        this.state = state;
    }
}
