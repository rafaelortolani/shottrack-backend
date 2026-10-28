package com.shottrack.backend.application.dashboard.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * ADR-0022: melhor valor do atleta num tipo de resultado DENTRO de uma modalidade
 * (respeitando a orientação, ADR-0011), com a quantidade de registros válidos.
 * Alimenta os recordes filtrados por modalidade e a evolução sugerida
 * (tipo de resultado mais registrado, UC42 v7).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@Embeddable
public class ModalityRecord {

    @Column(name = "modality_id", nullable = false)
    private final UUID modalityId;

    @Column(name = "result_type_id", nullable = false)
    private final UUID resultTypeId;

    @Column(name = "result_type_name", nullable = false)
    private final String resultTypeName;

    @Column(name = "best_value", nullable = false)
    private final BigDecimal bestValue;

    @Column(name = "record_count", nullable = false)
    private final int recordCount;

    @Builder
    private ModalityRecord(UUID modalityId, UUID resultTypeId, String resultTypeName, BigDecimal bestValue,
                           int recordCount) {
        this.modalityId = modalityId;
        this.resultTypeId = resultTypeId;
        this.resultTypeName = resultTypeName;
        this.bestValue = bestValue;
        this.recordCount = recordCount;
    }
}
