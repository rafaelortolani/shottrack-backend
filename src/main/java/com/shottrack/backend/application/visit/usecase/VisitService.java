package com.shottrack.backend.application.visit.usecase;

import com.shottrack.backend.application.dashboard.event.DashboardRecalculationRequestedEvent;
import com.shottrack.backend.application.traininglocation.usecase.TrainingLocationService;
import com.shottrack.backend.application.visit.dto.StartVisitRequest;
import com.shottrack.backend.application.visit.dto.TrainingResponse;
import com.shottrack.backend.application.visit.dto.VisitResponse;
import com.shottrack.backend.application.visit.gateway.VisitGateway;
import com.shottrack.backend.application.visit.mapper.VisitMapper;
import com.shottrack.backend.application.visit.model.Visit;
import com.shottrack.backend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VisitService {

    private final VisitGateway visitGateway;
    private final TrainingLocationService trainingLocationService;
    private final TrainingService trainingService;
    private final VisitMapper visitMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

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
     * encerra também qualquer treino dela ainda EM_ANDAMENTO — sem exigir
     * confirmação adicional.
     * ADR-0015: também precisa de transação própria — mesmo motivo do
     * TrainingService.close (o evento publicado no fim só é entregue na
     * fila depois que este método inteiro commitar).
     */
    @Transactional
    public VisitResponse close(UUID userId, UUID visitId) {
        Visit visit = findOwnedVisitOrThrow(userId, visitId);

        if (!visit.isOpen()) {
            throw new BusinessException("VISIT_ALREADY_CLOSED", HttpStatus.CONFLICT);
        }

        Instant closedAt = Instant.now();
        visit.close(closedAt);
        trainingService.closeAllOpenByVisitId(visitId, closedAt);

        Visit saved = visitGateway.save(visit);
        applicationEventPublisher.publishEvent(new DashboardRecalculationRequestedEvent(userId));
        return toResponse(saved);
    }

    /**
     * UC44: sem bloqueio por status — excluir uma visita EM_ANDAMENTO é
     * permitido (corrigir engano, ex: local errado). Treinos e as séries
     * deles somem junto via ON DELETE CASCADE nas constraints (V38), não
     * explicitamente aqui.
     * ADR-0015: transação própria pelo mesmo motivo do close acima.
     */
    @Transactional
    public void delete(UUID userId, UUID visitId) {
        visitGateway.delete(findOwnedVisitOrThrow(userId, visitId));
        applicationEventPublisher.publishEvent(new DashboardRecalculationRequestedEvent(userId));
    }

    private Visit findOwnedVisitOrThrow(UUID userId, UUID visitId) {
        return visitGateway.findById(visitId)
                .filter(visit -> visit.getUserId().equals(userId))
                .orElseThrow(() -> new BusinessException("VISIT_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    private VisitResponse toResponse(Visit visit) {
        List<TrainingResponse> trainings = trainingService.listByVisitId(visit.getId());
        return visitMapper.toResponse(visit, trainings);
    }
}
