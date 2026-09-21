package com.shottrack.backend.application.visit.usecase;

import com.shottrack.backend.application.traininglocation.usecase.TrainingLocationService;
import com.shottrack.backend.application.visit.dto.StartVisitRequest;
import com.shottrack.backend.application.visit.dto.TrainingSummaryResponse;
import com.shottrack.backend.application.visit.dto.VisitResponse;
import com.shottrack.backend.application.visit.gateway.VisitGateway;
import com.shottrack.backend.application.visit.mapper.VisitMapper;
import com.shottrack.backend.application.visit.model.Visit;
import com.shottrack.backend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VisitService {

    private final VisitGateway visitGateway;
    private final TrainingLocationService trainingLocationService;
    private final VisitMapper visitMapper;

    public VisitResponse start(UUID userId, StartVisitRequest request) {
        trainingLocationService.findOwnedTrainingLocationOrThrow(userId, request.trainingLocationId());

        Visit visit = Visit.builder()
                .userId(userId)
                .trainingLocationId(request.trainingLocationId())
                .observations(request.observations())
                .build();

        return toResponse(visitGateway.save(visit));
    }

    public List<VisitResponse> listByUser(UUID userId) {
        return visitGateway.findAllByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * UC34/ADR-0012: encerra a visita e, na mesma operação (mesmo timestamp),
     * encerraria também qualquer treino dela ainda EM_ANDAMENTO. O domínio de
     * Treino ainda não existe (UC32/UC33), então não há o que cascatear de
     * verdade ainda — mesmo padrão já usado em UC08/UC27 pra pendências que
     * dependem de um domínio futuro.
     */
    public VisitResponse close(UUID userId, UUID visitId) {
        Visit visit = findOwnedVisitOrThrow(userId, visitId);

        if (!visit.isOpen()) {
            throw new BusinessException("VISIT_ALREADY_CLOSED", HttpStatus.CONFLICT);
        }

        visit.close(Instant.now());

        return toResponse(visitGateway.save(visit));
    }

    private Visit findOwnedVisitOrThrow(UUID userId, UUID visitId) {
        return visitGateway.findById(visitId)
                .filter(visit -> visit.getUserId().equals(userId))
                .orElseThrow(() -> new BusinessException("VISIT_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    private VisitResponse toResponse(Visit visit) {
        return visitMapper.toResponse(visit, trainingsFor(visit));
    }

    /**
     * Treino ainda não existe (UC32/UC33/ADR-0012) — toda visita é retornada
     * sem nenhum treino aninhado por enquanto. Troca-se essa lista vazia pela
     * consulta real assim que o domínio existir, sem reescrever o restante
     * do mapeamento.
     */
    private List<TrainingSummaryResponse> trainingsFor(Visit visit) {
        return List.of();
    }
}
