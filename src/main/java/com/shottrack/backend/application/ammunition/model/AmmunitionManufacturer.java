package com.shottrack.backend.application.ammunition.model;

import com.shottrack.backend.common.jpa.AbstractBaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Catálogo fixo, populado via migration (ADR-0007) — sem endpoint de cadastro.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@Entity
@Table(name = "ammunition_manufacturers")
public class AmmunitionManufacturer extends AbstractBaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private final String name;

    @Builder
    private AmmunitionManufacturer(String name) {
        this.name = name;
    }
}
