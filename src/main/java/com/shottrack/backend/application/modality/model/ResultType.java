package com.shottrack.backend.application.modality.model;

import com.shottrack.backend.common.jpa.AbstractBaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Catálogo fixo, populado via migration (ADR-0011) — sem endpoint de cadastro.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@Entity
@Table(name = "result_types")
public class ResultType extends AbstractBaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private final String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private final ResultOrientation orientation;

    @Builder
    private ResultType(String name, ResultOrientation orientation) {
        this.name = name;
        this.orientation = orientation;
    }
}
