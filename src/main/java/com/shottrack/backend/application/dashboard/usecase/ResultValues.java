package com.shottrack.backend.application.dashboard.usecase;

import com.shottrack.backend.application.modality.model.ResultOrientation;
import com.shottrack.backend.common.validation.SafeDecimal;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

/**
 * Regras de valor de resultado compartilhadas por destaque, recordes
 * (UC42), evolução (UC46) e comparação de armas (UC48), pra não haver
 * duas interpretações diferentes de "valor numérico" ou de "melhor valor".
 */
final class ResultValues {

    private ResultValues() {
    }

    /**
     * ADR-0013: valor de resultado é texto — não numérico é ignorado
     * silenciosamente nos cálculos (UC42/UC46/UC48, observação). S04: também
     * é ignorado o que a SafeDecimal recusa (notação científica, tamanho,
     * precisão/escala fora da faixa) — inclusive valores antigos já gravados.
     */
    static Optional<BigDecimal> parseNumeric(String value) {
        return SafeDecimal.parse(value);
    }

    /**
     * ADR-0011: melhor valor respeitando a orientação do tipo. Só faz
     * sentido pra MENOR_MELHOR/MAIOR_MELHOR — quem chama filtra
     * NAO_APLICAVEL antes.
     */
    static BigDecimal best(ResultOrientation orientation, Collection<BigDecimal> values) {
        if (orientation == ResultOrientation.NAO_APLICAVEL) {
            throw new IllegalArgumentException("Tipo sem orientação não tem melhor valor");
        }
        Comparator<BigDecimal> bestFirst = orientation == ResultOrientation.MENOR_MELHOR
                ? Comparator.naturalOrder()
                : Comparator.reverseOrder();
        return values.stream().min(bestFirst).orElseThrow();
    }
}
