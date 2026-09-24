package com.shottrack.backend.application.weapon.model;

import com.shottrack.backend.common.jpa.AbstractBaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Catálogo fixo, populado via migration (ADR-0004) — sem endpoint de cadastro.
 * Sempre pertence a uma marca (weapon_brands). Desde a Revisão 2 do
 * ADR-0004, o tipo é inerente ao modelo e o modelo define os calibres
 * válidos — a arma herda tipo/marca daqui em vez de o atleta escolher.
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

    @Column(name = "weapon_type_id", nullable = false)
    private final UUID weaponTypeId;

    @Column(nullable = false)
    private final String name;

    /**
     * ADR-0004 (Revisão 2): calibres permitidos pra esse modelo
     * (weapon_model_calibers). EAGER: é um catálogo pequeno, e a validação
     * do UC06/UC10 sempre precisa dele junto com o modelo.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "weapon_model_calibers", joinColumns = @JoinColumn(name = "weapon_model_id"))
    @Column(name = "weapon_caliber_id")
    private Set<UUID> caliberIds = new HashSet<>();

    @Builder
    private WeaponModel(UUID brandId, UUID weaponTypeId, String name) {
        this.brandId = brandId;
        this.weaponTypeId = weaponTypeId;
        this.name = name;
    }

    public boolean allowsCaliber(UUID caliberId) {
        return caliberIds.contains(caliberId);
    }
}
