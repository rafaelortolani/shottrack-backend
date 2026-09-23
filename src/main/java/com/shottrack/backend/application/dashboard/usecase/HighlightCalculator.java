package com.shottrack.backend.application.dashboard.usecase;

import com.shottrack.backend.application.modality.gateway.ResultTypeGateway;
import com.shottrack.backend.application.modality.model.ResultOrientation;
import com.shottrack.backend.application.modality.model.ResultType;
import com.shottrack.backend.application.series.model.SeriesResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * UC42/ADR-0011: regra única de "destaque", reaproveitada em três escopos
 * — o atleta inteiro, uma modalidade e um treino. O escopo é só o
 * conjunto de resultados recebido; a regra é sempre a mesma: entre os
 * tipos MENOR_MELHOR/MAIOR_MELHOR, o que tem mais registros preenchidos;
 * valor é o melhor já registrado nesse tipo, respeitando a orientação;
 * desempate pelo mais usado recentemente; vazio se nenhum tipo elegível
 * tiver registro válido.
 */
@Component
@RequiredArgsConstructor
public class HighlightCalculator {

    private final ResultTypeGateway resultTypeGateway;

    public Optional<ResultHighlight> calculate(Collection<SeriesResult> results) {
        if (results.isEmpty()) {
            return Optional.empty();
        }

        Map<UUID, ResultType> eligibleTypesById = resultTypeGateway.findAll().stream()
                .filter(type -> type.getOrientation() != ResultOrientation.NAO_APLICAVEL)
                .collect(Collectors.toMap(ResultType::getId, Function.identity()));

        return results.stream()
                .filter(result -> !result.isNotApplicable())
                .map(result -> toEntry(result, eligibleTypesById))
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(entry -> entry.type().getId()))
                .values().stream()
                .map(this::toCandidate)
                .max(Comparator.comparingInt(Candidate::count).thenComparing(Candidate::mostRecentlyUsedAt))
                .map(candidate -> new ResultHighlight(candidate.resultTypeName(), candidate.value()));
    }

    private Entry toEntry(SeriesResult result, Map<UUID, ResultType> eligibleTypesById) {
        ResultType type = eligibleTypesById.get(result.getResultTypeId());
        if (type == null) {
            return null;
        }

        BigDecimal numericValue = parseNumericOrNull(result.getValue());
        if (numericValue == null) {
            return null;
        }

        return new Entry(type, numericValue, result.getCreatedAt());
    }

    private Candidate toCandidate(List<Entry> entriesForType) {
        ResultType type = entriesForType.get(0).type();
        Comparator<BigDecimal> bestValueOrder = type.getOrientation() == ResultOrientation.MENOR_MELHOR
                ? Comparator.naturalOrder()
                : Comparator.reverseOrder();

        BigDecimal bestValue = entriesForType.stream().map(Entry::value).min(bestValueOrder).orElseThrow();
        Instant mostRecentlyUsedAt = entriesForType.stream().map(Entry::recordedAt).max(Comparator.naturalOrder()).orElseThrow();

        return new Candidate(type.getName(), bestValue, entriesForType.size(), mostRecentlyUsedAt);
    }

    /**
     * ADR-0013: valor de resultado é texto — não numérico pro tipo em
     * questão é ignorado silenciosamente no cálculo (UC42, observação).
     */
    private BigDecimal parseNumericOrNull(String value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private record Entry(ResultType type, BigDecimal value, Instant recordedAt) {
    }

    private record Candidate(String resultTypeName, BigDecimal value, int count, Instant mostRecentlyUsedAt) {
    }
}
