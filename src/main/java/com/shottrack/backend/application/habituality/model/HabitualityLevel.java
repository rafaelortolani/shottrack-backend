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
 * ADR-0023: nível dentro de uma versão. Substituído junto com a versão (UC55), por isso
 * o atleta guarda o nome do nível, não o id.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@Entity
@Table(name = "habituality_levels")
public class HabitualityLevel extends AbstractBaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "version_id", nullable = false)
    private final UUID versionId;

    @Column(nullable = false)
    private final String name;

    @Column(nullable = false)
    private final int position;

    @Builder
    private HabitualityLevel(UUID versionId, @NonNull String name, int position) {
        this.versionId = versionId;
        this.name = name;
        this.position = position;
    }
}
