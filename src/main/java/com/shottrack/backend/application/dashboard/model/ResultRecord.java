package com.shottrack.backend.application.dashboard.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * ADR-0016: recorde do atleta num tipo de resultado — o melhor valor já
 * registrado, respeitando a orientação (ADR-0011).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@Embeddable
public class ResultRecord {

    @Column(name = "result_type_name", nullable = false)
    private final String resultTypeName;

    @Column(name = "best_value", nullable = false)
    private final BigDecimal bestValue;

    @Builder
    private ResultRecord(String resultTypeName, BigDecimal bestValue) {
        this.resultTypeName = resultTypeName;
        this.bestValue = bestValue;
    }
}
