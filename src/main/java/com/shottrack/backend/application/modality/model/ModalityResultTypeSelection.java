package com.shottrack.backend.application.modality.model;

import com.shottrack.backend.common.jpa.AbstractBaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Perfil de modalidade (UC30/ADR-0011): tipo de resultado que o atleta
 * escolheu acompanhar numa modalidade praticada. Gravada como seleção do
 * atleta — não uma referência à sugestão padrão (ModalityResultTypeSuggestion) —
 * criada automaticamente a partir dela quando a modalidade é adicionada
 * (UC12), e livremente editável depois.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@Entity
@Table(name = "modality_result_type_selections")
public class ModalityResultTypeSelection extends AbstractBaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private final UUID userId;

    @Column(name = "modality_id", nullable = false)
    private final UUID modalityId;

    @Column(name = "result_type_id", nullable = false)
    private final UUID resultTypeId;

    @Builder
    private ModalityResultTypeSelection(UUID userId, UUID modalityId, UUID resultTypeId) {
        this.userId = userId;
        this.modalityId = modalityId;
        this.resultTypeId = resultTypeId;
    }
}
