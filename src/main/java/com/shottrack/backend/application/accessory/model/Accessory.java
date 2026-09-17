package com.shottrack.backend.application.accessory.model;

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
 * Cadastro livre (ADR-0008), exceto o tipo — catálogo fechado (ADR-0008,
 * revisão), mesmo padrão de tipo/marca/modelo/calibre de Arma (ADR-0004).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@Entity
@Table(name = "accessories")
public class Accessory extends AbstractBaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private final UUID userId;

    @Setter
    @NonNull
    @Column(nullable = false)
    private String name;

    @Setter
    @NonNull
    @Column(name = "type_id", nullable = false)
    private UUID typeId;

    @Setter
    @Column
    private String notes;

    @Builder
    private Accessory(UUID userId, @NonNull String name, @NonNull UUID typeId, String notes) {
        this.userId = userId;
        this.name = name;
        this.typeId = typeId;
        this.notes = notes;
    }
}
