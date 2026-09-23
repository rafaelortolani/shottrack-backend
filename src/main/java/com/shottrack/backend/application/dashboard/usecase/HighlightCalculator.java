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
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * UC42/ADR-0011: melhor valor por tipo de resultado, sobre um conjunto de
 * resultados que define o escopo (atleta inteiro, uma modalidade ou um
 * treino). Duas leituras da mesma apuração:
 * - records: o melhor valor de CADA tipo MENOR_MELHOR/MAIOR_MELHOR com
 *   registro válido (recordes do atleta, ADR-0016);
 * - calculate: só o destaque — o tipo com mais registros preenchidos,
 *   desempate pelo mais usado recentemente; vazio se nenhum tipo elegível
 *   tiver registro válido.
 */
@Component
@RequiredArgsConstructor
public class HighlightCalculator {

    private final ResultTypeGateway resultTypeGateway;

    public Optional<ResultHighlight> calculate(Collection<SeriesResult> results) {
        return candidates(results).stream()
                .max(Comparator.comparingInt(Candidate::count).thenComparing(Candidate::mostRecentlyUsedAt))
                .map(Candidate::toHighlight);
    }

    public List<ResultHighlight> records(Collection<SeriesResult> results) {
        return candidates(results).stream()
                .map(Candidate::toHighlight)
                .sorted(Comparator.comparing(ResultHighlight::resultTypeName))
                .toList();
    }

    private List<Candidate> candidates(Collection<SeriesResult> results) {
        if (results.isEmpty()) {
            return List.of();
        }

        Map<UUID, ResultType> eligibleTypesById = resultTypeGateway.findAll().stream()
                .filter(type -> type.getOrientation() != ResultOrientation.NAO_APLICAVEL)
                .collect(Collectors.toMap(ResultType::getId, Function.identity()));

        return results.stream()
                .filter(result -> !result.isNotApplicable())
                .map(result -> toEntry(result, eligibleTypesById))
                .flatMap(Optional::stream)
                .collect(Collectors.groupingBy(entry -> entry.type().getId()))
                .values().stream()
                .map(this::toCandidate)
                .toList();
    }

    private Optional<Entry> toEntry(SeriesResult result, Map<UUID, ResultType> eligibleTypesById) {
        ResultType type = eligibleTypesById.get(result.getResultTypeId());
        if (type == null) {
            return Optional.empty();
        }

        return ResultValues.parseNumeric(result.getValue())
                .map(value -> new Entry(type, value, result.getCreatedAt()));
    }

    private Candidate toCandidate(List<Entry> entriesForType) {
        ResultType type = entriesForType.get(0).type();
        BigDecimal bestValue = ResultValues.best(type.getOrientation(), entriesForType.stream().map(Entry::value).toList());
        Instant mostRecentlyUsedAt = entriesForType.stream().map(Entry::recordedAt).max(Comparator.naturalOrder()).orElseThrow();

        return new Candidate(type.getName(), bestValue, entriesForType.size(), mostRecentlyUsedAt);
    }

    private record Entry(ResultType type, BigDecimal value, Instant recordedAt) {
    }

    private record Candidate(String resultTypeName, BigDecimal value, int count, Instant mostRecentlyUsedAt) {

        ResultHighlight toHighlight() {
            return new ResultHighlight(resultTypeName, value);
        }
    }
}
