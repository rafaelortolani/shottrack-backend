package com.shottrack.backend.application.modality.usecase;

import com.shottrack.backend.application.modality.dto.ModalityResponse;
import com.shottrack.backend.application.modality.gateway.ModalityGateway;
import com.shottrack.backend.application.modality.gateway.PracticedModalityGateway;
import com.shottrack.backend.application.modality.mapper.ModalityMapper;
import com.shottrack.backend.application.modality.model.Modality;
import com.shottrack.backend.application.modality.model.PracticedModality;
import com.shottrack.backend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PracticedModalityService {

    private final PracticedModalityGateway practicedModalityGateway;
    private final ModalityGateway modalityGateway;
    private final ModalityResultTypeService modalityResultTypeService;
    private final ModalityMapper modalityMapper;

    /**
     * UC12/ADR-0011: ao adicionar, aplica automaticamente a sugestão padrão
     * de tipos de resultado da modalidade como seleção inicial do atleta —
     * efeito colateral interno, sem mudar a interface pública deste endpoint
     * (o atleta só vê o resultado; ajustar depois é UC30).
     */
    public ModalityResponse add(UUID userId, UUID modalityId) {
        Modality modality = modalityGateway.findById(modalityId)
                .orElseThrow(() -> new BusinessException("MODALITY_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (practicedModalityGateway.existsByUserIdAndModalityId(userId, modalityId)) {
            throw new BusinessException("MODALITY_ALREADY_ADDED", HttpStatus.CONFLICT);
        }

        practicedModalityGateway.save(PracticedModality.builder().userId(userId).modalityId(modalityId).build());
        modalityResultTypeService.applyDefaultSuggestion(userId, modalityId);
        return modalityMapper.toResponse(modality);
    }

    public void remove(UUID userId, UUID modalityId) {
        PracticedModality practicedModality = practicedModalityGateway.findByUserIdAndModalityId(userId, modalityId)
                .orElseThrow(() -> new BusinessException("MODALITY_NOT_ASSOCIATED", HttpStatus.NOT_FOUND));

        practicedModalityGateway.delete(practicedModality);
    }

    public List<ModalityResponse> listByUser(UUID userId) {
        return practicedModalityGateway.findAllByUserId(userId).stream()
                .map(practiced -> modalityGateway.findById(practiced.getModalityId()).orElseThrow())
                .map(modalityMapper::toResponse)
                .toList();
    }
}
