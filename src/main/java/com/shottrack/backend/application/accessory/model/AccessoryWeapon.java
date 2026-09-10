package com.shottrack.backend.application.accessory.model;

import com.shottrack.backend.common.jpa.AbstractBaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Associação N:N entre acessório e arma (UC19/ADR-0008).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@Entity
@Table(name = "accessory_weapons")
public class AccessoryWeapon extends AbstractBaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "accessory_id", nullable = false)
    private final UUID accessoryId;

    @Column(name = "weapon_id", nullable = false)
    private final UUID weaponId;

    @Builder
    private AccessoryWeapon(UUID accessoryId, UUID weaponId) {
        this.accessoryId = accessoryId;
        this.weaponId = weaponId;
    }
}
