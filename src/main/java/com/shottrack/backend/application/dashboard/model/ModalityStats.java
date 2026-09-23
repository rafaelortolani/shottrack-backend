package com.shottrack.backend.application.dashboard.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * UC42 (Onda 1): uma linha do resumo de modalidades — contagem de treinos
 * na modalidade e o destaque (ADR-0011) escopado só às séries dela.
 * bestResultTypeName/bestValue nulos = nenhum resultado elegível ainda.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@Embeddable
public class ModalityStats {

    @Column(name = "modality_name", nullable = false)
    private final String modalityName;

    @Column(name = "training_count", nullable = false)
    private final int trainingCount;

    @Column(name = "best_result_type_name")
    private final String bestResultTypeName;

    @Column(name = "best_value")
    private final BigDecimal bestValue;

    @Builder
    private ModalityStats(String modalityName, int trainingCount, String bestResultTypeName, BigDecimal bestValue) {
        this.modalityName = modalityName;
        this.trainingCount = trainingCount;
        this.bestResultTypeName = bestResultTypeName;
        this.bestValue = bestValue;
    }
}
