package com.shottrack.backend.application.series.model;

import com.shottrack.backend.common.jpa.AbstractBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Série (ADR-0013): menor unidade de registro, pertence a um Treino.
 * Nenhum campo além de trainingId é obrigatório — "registrar primeiro,
 * organizar depois".
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@Entity
@Table(name = "series")
public class Series extends AbstractBaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "training_id", nullable = false)
    private final UUID trainingId;

    @Setter
    @Column(name = "weapon_id")
    private UUID weaponId;

    @Setter
    @Column(name = "ammunition_id")
    private UUID ammunitionId;

    @Setter
    @Column(name = "distance_meters")
    private BigDecimal distanceMeters;

    @Setter
    @Column
    private String target;

    @Setter
    @Column(name = "shot_count")
    private Integer shotCount;

    @Setter
    @Column
    private String notes;

    @Builder
    private Series(UUID trainingId, UUID weaponId, UUID ammunitionId, BigDecimal distanceMeters,
                    String target, Integer shotCount, String notes) {
        this.trainingId = trainingId;
        this.weaponId = weaponId;
        this.ammunitionId = ammunitionId;
        this.distanceMeters = distanceMeters;
        this.target = target;
        this.shotCount = shotCount;
        this.notes = notes;
    }
}
