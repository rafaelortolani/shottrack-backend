package com.shottrack.backend.application.accessory.model;

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
 * Cadastro livre (ADR-0008) — sem catálogo fechado, diferente de Arma.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@Entity
@Table(name = "accessories")
public class Accessory extends AbstractBaseEntity {

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
    @Column
    private String type;

    @Setter
    @Column
    private String notes;

    @Builder
    private Accessory(UUID userId, @NonNull String name, String type, String notes) {
        this.userId = userId;
        this.name = name;
        this.type = type;
        this.notes = notes;
    }
}
