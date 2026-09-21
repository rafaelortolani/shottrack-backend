package com.shottrack.backend.application.modality.usecase;

import com.shottrack.backend.application.modality.dto.ResultTypeResponse;
import com.shottrack.backend.application.modality.gateway.ModalityResultTypeSelectionGateway;
import com.shottrack.backend.application.modality.gateway.ModalityResultTypeSuggestionGateway;
import com.shottrack.backend.application.modality.gateway.PracticedModalityGateway;
import com.shottrack.backend.application.modality.gateway.ResultTypeGateway;
import com.shottrack.backend.application.modality.mapper.ModalityMapper;
import com.shottrack.backend.application.modality.model.ModalityResultTypeSelection;
import com.shottrack.backend.application.modality.model.ResultType;
import com.shottrack.backend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ModalityResultTypeService {

    private final PracticedModalityGateway practicedModalityGateway;
    private final ResultTypeGateway resultTypeGateway;
    private final ModalityResultTypeSuggestionGateway modalityResultTypeSuggestionGateway;
    private final ModalityResultTypeSelectionGateway modalityResultTypeSelectionGateway;
    private final ModalityMapper modalityMapper;

    public List<ResultTypeResponse> listConfigured(UUID userId, UUID modalityId) {
        assertPracticed(userId, modalityId);

        return modalityResultTypeSelectionGateway.findAllByUserIdAndModalityId(userId, modalityId).stream()
                .map(selection -> findResultTypeOrThrow(selection.getResultTypeId()))
                .map(modalityMapper::toResponse)
                .toList();
    }

    public ResultTypeResponse add(UUID userId, UUID modalityId, UUID resultTypeId) {
        assertPracticed(userId, modalityId);
        ResultType resultType = findResultTypeOrThrow(resultTypeId);

        if (modalityResultTypeSelectionGateway.existsByUserIdAndModalityIdAndResultTypeId(userId, modalityId, resultTypeId)) {
            throw new BusinessException("RESULT_TYPE_ALREADY_CONFIGURED", HttpStatus.CONFLICT);
        }

        modalityResultTypeSelectionGateway.save(ModalityResultTypeSelection.builder()
                .userId(userId)
                .modalityId(modalityId)
                .resultTypeId(resultTypeId)
                .build());

        return modalityMapper.toResponse(resultType);
    }

    public void remove(UUID userId, UUID modalityId, UUID resultTypeId) {
        assertPracticed(userId, modalityId);

        ModalityResultTypeSelection selection = modalityResultTypeSelectionGateway
                .findByUserIdAndModalityIdAndResultTypeId(userId, modalityId, resultTypeId)
                .orElseThrow(() -> new BusinessException("RESULT_TYPE_NOT_CONFIGURED", HttpStatus.NOT_FOUND));

        modalityResultTypeSelectionGateway.delete(selection);
    }

    /**
     * UC12/ADR-0011: aplicada automaticamente quando o atleta adiciona uma
     * modalidade praticada — grava a sugestão padrão como seleção do atleta
     * (não uma referência à sugestão), ponto de partida livremente editável
     * depois (UC30).
     */
    public void applyDefaultSuggestion(UUID userId, UUID modalityId) {
        modalityResultTypeSuggestionGateway.findAllByModalityId(modalityId).forEach(suggestion ->
                modalityResultTypeSelectionGateway.save(ModalityResultTypeSelection.builder()
                        .userId(userId)
                        .modalityId(modalityId)
                        .resultTypeId(suggestion.getResultTypeId())
                        .build()));
    }

    private void assertPracticed(UUID userId, UUID modalityId) {
        if (!practicedModalityGateway.existsByUserIdAndModalityId(userId, modalityId)) {
            throw new BusinessException("MODALITY_NOT_PRACTICED", HttpStatus.NOT_FOUND);
        }
    }

    private ResultType findResultTypeOrThrow(UUID resultTypeId) {
        return resultTypeGateway.findById(resultTypeId)
                .orElseThrow(() -> new BusinessException("RESULT_TYPE_NOT_FOUND", HttpStatus.NOT_FOUND));
    }
}
