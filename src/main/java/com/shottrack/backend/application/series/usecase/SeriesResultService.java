package com.shottrack.backend.application.series.usecase;

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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
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

    private final SeriesResultGateway seriesResultGateway;
    private final SeriesGateway seriesGateway;
    private final TrainingGateway trainingGateway;
    private final TrainingService trainingService;
    private final ModalityResultTypeSelectionGateway modalityResultTypeSelectionGateway;
    private final ResultTypeGateway resultTypeGateway;
    private final SeriesResultMapper seriesResultMapper;

    /**
     * UC39 — fluxo "registrar valor": substitui um registro anterior pra
     * esse tipo, inclusive se estava marcado "não aplicável".
     */
    public SeriesResultResponse registerValue(UUID userId, UUID seriesId, UUID resultTypeId, String value) {
        Series series = findOwnedSeriesOrThrow(userId, seriesId);
        ResultType resultType = assertConfiguredForTrainingModality(userId, series, resultTypeId);

        validateValueFormat(resultType, value);

        SeriesResult result = findOrCreateResult(series.getId(), resultTypeId);
        result.registerValue(value);

        return toResponse(seriesResultGateway.save(result), resultType);
    }

    /**
     * UC39 — fluxo "marcar como não aplicável": substitui um valor anterior,
     * se havia.
     */
    public SeriesResultResponse markNotApplicable(UUID userId, UUID seriesId, UUID resultTypeId) {
        Series series = findOwnedSeriesOrThrow(userId, seriesId);
        ResultType resultType = assertConfiguredForTrainingModality(userId, series, resultTypeId);

        SeriesResult result = findOrCreateResult(series.getId(), resultTypeId);
        result.markNotApplicable();

        return toResponse(seriesResultGateway.save(result), resultType);
    }

    /**
     * UC40: remove o registro — o tipo volta ao estado "não preenchido"
     * (deixa de existir, não vira "0" nem "não aplicável").
     */
    public void remove(UUID userId, UUID seriesId, UUID resultTypeId) {
        Series series = findOwnedSeriesOrThrow(userId, seriesId);

        SeriesResult result = seriesResultGateway.findBySeriesIdAndResultTypeId(series.getId(), resultTypeId)
                .orElseThrow(() -> new BusinessException("RESULT_NOT_CONFIGURED", HttpStatus.NOT_FOUND));

        seriesResultGateway.delete(result);
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
