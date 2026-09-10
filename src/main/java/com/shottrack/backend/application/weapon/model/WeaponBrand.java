package com.shottrack.backend.application.weapon.model;

import com.shottrack.backend.common.jpa.AbstractBaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Catálogo fixo, populado via migration (ADR-0004) — sem endpoint de cadastro.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@Entity
@Table(name = "weapon_brands")
public class WeaponBrand extends AbstractBaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private final String name;

    @Builder
    private WeaponBrand(String name) {
        this.name = name;
    }
}
