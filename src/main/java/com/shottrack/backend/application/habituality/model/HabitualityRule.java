package com.shottrack.backend.application.habituality.model;

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
 * ADR-0023: regra de habitualidade (ex: uma norma). Nunca é excluída, só desativada —
 * atletas podem estar acompanhando-a.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@Entity
@Table(name = "habituality_rules")
public class HabitualityRule extends AbstractBaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Setter
    @NonNull
    @Column(nullable = false)
    private String name;

    @Setter
    @Column(name = "legal_reference")
    private String legalReference;

    @Setter
    @Column(nullable = false)
    private boolean active = true;

    @Builder
    private HabitualityRule(@NonNull String name, String legalReference, Boolean active) {
        this.name = name;
        this.legalReference = legalReference;
        if (active != null) {
            this.active = active;
        }
    }
}
