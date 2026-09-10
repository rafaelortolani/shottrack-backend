package com.shottrack.backend.application.modality.model;

import com.shottrack.backend.common.jpa.AbstractBaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Associação entre um atleta e uma modalidade do catálogo (UC12) — a
 * modalidade que ele pratica.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@Entity
@Table(name = "practiced_modalities")
public class PracticedModality extends AbstractBaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private final UUID userId;

    @Column(name = "modality_id", nullable = false)
    private final UUID modalityId;

    @Builder
    private PracticedModality(UUID userId, UUID modalityId) {
        this.userId = userId;
        this.modalityId = modalityId;
    }
}
