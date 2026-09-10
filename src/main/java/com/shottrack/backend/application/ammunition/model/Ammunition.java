package com.shottrack.backend.application.ammunition.model;

import com.shottrack.backend.common.jpa.AbstractBaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Cadastro parcial (ADR-0007): nenhum campo além de userId é obrigatório
 * isoladamente — a regra de "fabricante OU apelido" é validada no usecase.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@Entity
@Table(name = "ammunitions")
public class Ammunition extends AbstractBaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private final UUID userId;

    @Setter
    @Column(name = "manufacturer_id")
    private UUID manufacturerId;

    @Setter
    @Column(name = "caliber_id")
    private UUID caliberId;

    @Setter
    @Column
    private String nickname;

    @Setter
    @Column(name = "projectile_weight_grains")
    private BigDecimal projectileWeightGrains;

    @Setter
    @Column(name = "powder_charge")
    private BigDecimal powderCharge;

    @Setter
    @Column(name = "projectile_type")
    private String projectileType;

    @Setter
    @Column
    private String lot;

    @Setter
    @Column
    private String notes;

    @Builder
    private Ammunition(UUID userId, UUID manufacturerId, UUID caliberId, String nickname,
                        BigDecimal projectileWeightGrains, BigDecimal powderCharge,
                        String projectileType, String lot, String notes) {
        this.userId = userId;
        this.manufacturerId = manufacturerId;
        this.caliberId = caliberId;
        this.nickname = nickname;
        this.projectileWeightGrains = projectileWeightGrains;
        this.powderCharge = powderCharge;
        this.projectileType = projectileType;
        this.lot = lot;
        this.notes = notes;
    }
}
