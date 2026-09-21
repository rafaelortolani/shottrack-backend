package com.shottrack.backend.application.series.model;

import com.shottrack.backend.common.jpa.AbstractBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Resultado de série (ADR-0013): três estados possíveis pra um tipo de
 * resultado numa série — sem linha (não preenchido), linha com value
 * preenchido (valor de verdade, inclusive "0"), ou linha com
 * notApplicable=true e value nulo (não aplicável). value/notApplicable só
 * mudam juntos, via os métodos de domínio abaixo — nunca setados soltos,
 * pra nunca existir uma linha com os dois preenchidos ao mesmo tempo.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@Entity
@Table(name = "series_results")
public class SeriesResult extends AbstractBaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "series_id", nullable = false)
    private final UUID seriesId;

    @Column(name = "result_type_id", nullable = false)
    private final UUID resultTypeId;

    @Column
    private String value;

    @Column(name = "not_applicable", nullable = false)
    private boolean notApplicable;

    @Builder
    private SeriesResult(UUID seriesId, UUID resultTypeId) {
        this.seriesId = seriesId;
        this.resultTypeId = resultTypeId;
    }

    /**
     * UC39: registra um valor de verdade — substitui um "não aplicável"
     * anterior, se havia.
     */
    public void registerValue(String value) {
        this.value = value;
        this.notApplicable = false;
    }

    /**
     * UC39: marca como não aplicável — substitui um valor anterior, se
     * havia.
     */
    public void markNotApplicable() {
        this.value = null;
        this.notApplicable = true;
    }
}
