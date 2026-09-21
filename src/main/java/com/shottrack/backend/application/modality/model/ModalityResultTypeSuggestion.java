package com.shottrack.backend.application.modality.model;

import com.shottrack.backend.common.jpa.AbstractBaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Sugestão padrão de tipos de resultado por modalidade (ADR-0011) — tabela
 * de seed, populada via migration, sem endpoint de cadastro. Consultada só
 * internamente por PracticedModalityService ao adicionar uma modalidade
 * praticada (UC12).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@Entity
@Table(name = "modality_result_type_suggestions")
public class ModalityResultTypeSuggestion extends AbstractBaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "modality_id", nullable = false)
    private final UUID modalityId;

    @Column(name = "result_type_id", nullable = false)
    private final UUID resultTypeId;

    @Builder
    private ModalityResultTypeSuggestion(UUID modalityId, UUID resultTypeId) {
        this.modalityId = modalityId;
        this.resultTypeId = resultTypeId;
    }
}
