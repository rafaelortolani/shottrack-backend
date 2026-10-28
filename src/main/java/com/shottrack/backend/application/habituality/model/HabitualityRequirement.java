package com.shottrack.backend.application.habituality.model;

import com.shottrack.backend.common.jpa.AbstractBaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

import com.shottrack.backend.application.visit.model.CompetitionScope;

import java.util.UUID;

/**
 * ADR-0023: exigência de um nível. Todas as exigências do nível precisam ser cumpridas.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@Entity
@Table(name = "habituality_requirements")
public class HabitualityRequirement extends AbstractBaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "level_id", nullable = false)
    private final UUID levelId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false)
    private final ActivityType activityType;

    @Column(name = "min_quantity", nullable = false)
    private final int minQuantity;

    @Column(name = "only_in_club", nullable = false)
    private final boolean onlyInClub;

    @Column(name = "distinct_events", nullable = false)
    private final boolean distinctEvents;

    @Enumerated(EnumType.STRING)
    @Column(name = "min_scope")
    private final CompetitionScope minScope;

    @Builder
    private HabitualityRequirement(UUID levelId, @NonNull ActivityType activityType, int minQuantity,
                                   boolean onlyInClub, boolean distinctEvents, CompetitionScope minScope) {
        this.levelId = levelId;
        this.activityType = activityType;
        this.minQuantity = minQuantity;
        this.onlyInClub = onlyInClub;
        this.distinctEvents = distinctEvents;
        this.minScope = minScope;
    }
}
