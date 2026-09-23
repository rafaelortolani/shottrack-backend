package com.shottrack.backend.application.series.usecase;

import com.shottrack.backend.application.dashboard.event.DashboardRecalculationRequestedEvent;
import com.shottrack.backend.application.modality.gateway.ModalityResultTypeSelectionGateway;
import com.shottrack.backend.application.modality.gateway.ResultTypeGateway;
import com.shottrack.backend.application.modality.model.ResultType;
import com.shottrack.backend.application.series.dto.SeriesResultResponse;
import com.shottrack.backend.application.series.gateway.SeriesGateway;
import com.shottrack.backend.application.series.gateway.SeriesResultGateway;
import com.shottrack.backend.application.series.mapper.SeriesResultMapper;
import com.shottrack.backend.application.series.model.Series;
import com.shottrack.backend.application.series.model.SeriesResult;
import com.shottrack.backend.application.visit.gateway.TrainingGateway;
import com.shottrack.backend.application.visit.model.Training;
import com.shottrack.backend.application.visit.usecase.TrainingService;
import com.shottrack.backend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Depende diretamente de SeriesGateway e TrainingGateway/TrainingService
 * (não de SeriesService) pra evitar dependência circular: SeriesService
 * reaproveita este serviço (listBySeriesId) pra montar os resultados
 * aninhados de UC37.
 */
@Service
@RequiredArgsConstructor
public class SeriesResultService {

    /**
     * ADR-0014: únicos tipos de resultado que contam disparos da série —
     * mantidos em consistência com Series.shotCount.
     */
    private static final Set<String> SHOT_RELATED_RESULT_TYPE_NAMES = Set.of("Acertos", "Erros");

    private final SeriesResultGateway seriesResultGateway;
    private final SeriesGateway seriesGateway;
    private final TrainingGateway trainingGateway;
    private final TrainingService trainingService;
    private final ModalityResultTypeSelectionGateway modalityResultTypeSelectionGateway;
    private final ResultTypeGateway resultTypeGateway;
    private final SeriesResultMapper seriesResultMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * UC39 — fluxo "registrar valor": substitui um registro anterior pra
     * esse tipo, inclusive se estava marcado "não aplicável".
     * ADR-0015: precisa de transação própria pra que o evento publicado no
     * fim só seja entregue na fila depois que este método inteiro commitar
     * (ver SeriesService.register).
     */
    @Transactional
    public SeriesResultResponse registerValue(UUID userId, UUID seriesId, UUID resultTypeId, String value) {
        Series series = findOwnedSeriesOrThrow(userId, seriesId);
        ResultType resultType = assertConfiguredForTrainingModality(userId, series, resultTypeId);

        validateValueFormat(resultType, value);
        enforceShotCountConsistency(series, resultType, contributionFromValue(resultType, value));

        SeriesResult result = findOrCreateResult(series.getId(), resultTypeId);
        result.registerValue(value);

        SeriesResult saved = seriesResultGateway.save(result);
        applicationEventPublisher.publishEvent(new DashboardRecalculationRequestedEvent(userId));
        return toResponse(saved, resultType);
    }

    /**
     * UC39 — fluxo "marcar como não aplicável": substitui um valor anterior,
     * se havia.
     * ADR-0015: também precisa de transação própria — mesmo motivo do
     * registerValue acima.
     */
    @Transactional
    public SeriesResultResponse markNotApplicable(UUID userId, UUID seriesId, UUID resultTypeId) {
        Series series = findOwnedSeriesOrThrow(userId, seriesId);
        ResultType resultType = assertConfiguredForTrainingModality(userId, series, resultTypeId);

        enforceShotCountConsistency(series, resultType, BigDecimal.ZERO);

        SeriesResult result = findOrCreateResult(series.getId(), resultTypeId);
        result.markNotApplicable();

        SeriesResult saved = seriesResultGateway.save(result);
        applicationEventPublisher.publishEvent(new DashboardRecalculationRequestedEvent(userId));
        return toResponse(saved, resultType);
    }

    /**
     * UC40: remove o registro — o tipo volta ao estado "não preenchido"
     * (deixa de existir, não vira "0" nem "não aplicável").
     * ADR-0015: também precisa de transação própria — mesmo motivo do
     * registerValue acima.
     */
    @Transactional
    public void remove(UUID userId, UUID seriesId, UUID resultTypeId) {
        Series series = findOwnedSeriesOrThrow(userId, seriesId);

        SeriesResult result = seriesResultGateway.findBySeriesIdAndResultTypeId(series.getId(), resultTypeId)
                .orElseThrow(() -> new BusinessException("RESULT_NOT_CONFIGURED", HttpStatus.NOT_FOUND));
        ResultType resultType = resultTypeGateway.findById(resultTypeId).orElseThrow();

        seriesResultGateway.delete(result);
        enforceShotCountConsistency(series, resultType, BigDecimal.ZERO);
        applicationEventPublisher.publishEvent(new DashboardRecalculationRequestedEvent(userId));
    }

    /**
     * UC38/ADR-0014: soma de acertos+erros já registrados pra série — usada
     * pra bloquear redução de quantidadeDisparos abaixo do que já existe.
     */
    public BigDecimal sumShotRelatedResults(UUID seriesId) {
        return SHOT_RELATED_RESULT_TYPE_NAMES.stream()
                .map(name -> shotRelatedContribution(seriesId, name))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * ADR-0014: acertos/erros mantidos em consistência com
     * Series.shotCount a cada mudança nesses dois tipos — enquanto o
     * atleta não tiver informado disparos manualmente (UC36/UC38), o valor
     * é recalculado a partir da soma; depois de informado manualmente, a
     * soma não pode ultrapassá-lo.
     */
    private void enforceShotCountConsistency(Series series, ResultType resultType, BigDecimal newContribution) {
        if (!SHOT_RELATED_RESULT_TYPE_NAMES.contains(resultType.getName())) {
            return;
        }

        BigDecimal total = newContribution.add(shotRelatedContributionExcluding(series.getId(), resultType.getName()));

        if (series.isShotCountSetManually()) {
            if (series.getShotCount() != null && total.compareTo(BigDecimal.valueOf(series.getShotCount())) > 0) {
                throw new BusinessException("RESULT_EXCEEDS_SHOT_COUNT", HttpStatus.BAD_REQUEST);
            }
        } else {
            series.autoFillShotCount(total.intValueExact());
            seriesGateway.save(series);
        }
    }

    private BigDecimal contributionFromValue(ResultType resultType, String value) {
        return SHOT_RELATED_RESULT_TYPE_NAMES.contains(resultType.getName()) ? new BigDecimal(value) : BigDecimal.ZERO;
    }

    private BigDecimal shotRelatedContributionExcluding(UUID seriesId, String excludingTypeName) {
        return SHOT_RELATED_RESULT_TYPE_NAMES.stream()
                .filter(name -> !name.equals(excludingTypeName))
                .map(name -> shotRelatedContribution(seriesId, name))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal shotRelatedContribution(UUID seriesId, String typeName) {
        return resultTypeGateway.findByName(typeName)
                .flatMap(type -> seriesResultGateway.findBySeriesIdAndResultTypeId(seriesId, type.getId()))
                .filter(result -> !result.isNotApplicable())
                .map(result -> new BigDecimal(result.getValue()))
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Reaproveitado por SeriesService (UC37) pra montar a lista de
     * resultados aninhados de uma série — não preenchido é ausência de
     * registro, então só aparecem os preenchidos/não aplicáveis.
     */
    public List<SeriesResultResponse> listBySeriesId(UUID seriesId) {
        return seriesResultGateway.findAllBySeriesId(seriesId).stream()
                .map(result -> toResponse(result, resultTypeGateway.findById(result.getResultTypeId()).orElseThrow()))
                .toList();
    }

    private SeriesResult findOrCreateResult(UUID seriesId, UUID resultTypeId) {
        return seriesResultGateway.findBySeriesIdAndResultTypeId(seriesId, resultTypeId)
                .orElseGet(() -> SeriesResult.builder().seriesId(seriesId).resultTypeId(resultTypeId).build());
    }

    /**
     * UC39/ADR-0013: um resultado só pode ser registrado pra um tipo que
     * esteja configurado no Perfil de Modalidade (UC30) da modalidade do
     * treino daquela série.
     */
    private ResultType assertConfiguredForTrainingModality(UUID userId, Series series, UUID resultTypeId) {
        Training training = trainingGateway.findById(series.getTrainingId()).orElseThrow();

        if (!modalityResultTypeSelectionGateway.existsByUserIdAndModalityIdAndResultTypeId(userId, training.getModalityId(), resultTypeId)) {
            throw new BusinessException("RESULT_TYPE_NOT_CONFIGURED_FOR_TRAINING", HttpStatus.NOT_FOUND);
        }

        return resultTypeGateway.findById(resultTypeId).orElseThrow();
    }

    /**
     * ADR-0013: não há uma coluna de formato no catálogo de tipos de
     * resultado — a interpretação do valor (numérico, sim/não, texto livre)
     * é derivada do próprio tipo, hardcoded aqui porque o catálogo é fechado
     * e pequeno (ADR-0011).
     */
    private void validateValueFormat(ResultType resultType, String value) {
        switch (resultType.getName()) {
            case "Exercício concluído" -> {
                if (!value.equalsIgnoreCase("sim") && !value.equalsIgnoreCase("não")) {
                    throw new BusinessException("RESULT_VALUE_FORMAT_INVALID", HttpStatus.BAD_REQUEST);
                }
            }
            case "Anotação livre" -> {
                // texto livre — qualquer valor não vazio é aceito
            }
            case "Acertos", "Erros" -> {
                // ADR-0014: contam disparos da série — precisam ser inteiro não-negativo
                try {
                    BigDecimal parsed = new BigDecimal(value);
                    if (parsed.signum() < 0 || parsed.remainder(BigDecimal.ONE).signum() != 0) {
                        throw new BusinessException("RESULT_VALUE_FORMAT_INVALID", HttpStatus.BAD_REQUEST);
                    }
                } catch (NumberFormatException e) {
                    throw new BusinessException("RESULT_VALUE_FORMAT_INVALID", HttpStatus.BAD_REQUEST);
                }
            }
            default -> {
                try {
                    new BigDecimal(value);
                } catch (NumberFormatException e) {
                    throw new BusinessException("RESULT_VALUE_FORMAT_INVALID", HttpStatus.BAD_REQUEST);
                }
            }
        }
    }

    private Series findOwnedSeriesOrThrow(UUID userId, UUID seriesId) {
        Series series = seriesGateway.findById(seriesId)
                .orElseThrow(() -> new BusinessException("SERIES_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!trainingService.isOwnedByUser(series.getTrainingId(), userId)) {
            throw new BusinessException("SERIES_NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        return series;
    }

    private SeriesResultResponse toResponse(SeriesResult seriesResult, ResultType resultType) {
        return seriesResultMapper.toResponse(seriesResult, resultType);
    }
}
