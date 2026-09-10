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
 * Sempre pertence a uma marca (weapon_brands).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@Entity
@Table(name = "weapon_models")
public class WeaponModel extends AbstractBaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "brand_id", nullable = false)
    private final UUID brandId;

    @Column(nullable = false)
    private final String name;

    @Builder
    private WeaponModel(UUID brandId, String name) {
        this.brandId = brandId;
        this.name = name;
    }
}
