package com.shottrack.backend.application.visit.usecase;

import com.shottrack.backend.application.dashboard.event.DashboardRecalculationRequestedEvent;
import com.shottrack.backend.application.modality.gateway.ModalityGateway;
import com.shottrack.backend.application.modality.gateway.PracticedModalityGateway;
import com.shottrack.backend.application.modality.model.Modality;
import com.shottrack.backend.application.visit.dto.OpenTrainingRequest;
import com.shottrack.backend.application.visit.dto.TrainingResponse;
import com.shottrack.backend.application.visit.gateway.TrainingGateway;
import com.shottrack.backend.application.visit.gateway.VisitGateway;
import com.shottrack.backend.application.visit.mapper.TrainingMapper;
import com.shottrack.backend.application.visit.model.Training;
import com.shottrack.backend.application.visit.model.TrainingStatus;
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

/**
 * Depende diretamente de VisitGateway (não de VisitService) pra evitar
 * dependência circular: VisitService reaproveita este serviço (listByVisitId)
 * pra montar os treinos aninhados de UC35.
 */
@Service
@RequiredArgsConstructor
public class TrainingService {

    private final TrainingGateway trainingGateway;
    private final VisitGateway visitGateway;
    private final PracticedModalityGateway practicedModalityGateway;
    private final ModalityGateway modalityGateway;
    private final TrainingMapper trainingMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    public TrainingResponse open(UUID userId, OpenTrainingRequest request) {
        Visit visit = findOwnedVisitOrThrow(userId, request.visitId());

        if (!visit.isOpen()) {
            throw new BusinessException("VISIT_ALREADY_CLOSED", HttpStatus.CONFLICT);
        }
        if (!practicedModalityGateway.existsByUserIdAndModalityId(userId, request.modalityId())) {
            throw new BusinessException("MODALITY_NOT_PRACTICED", HttpStatus.NOT_FOUND);
        }

        Training training = Training.builder()
                .visitId(visit.getId())
                .modalityId(request.modalityId())
                .build();

        return toResponse(trainingGateway.save(training));
    }

    /**
     * UC33/ADR-0012: encerrar um treino não exige que a visita ainda esteja
     * EM_ANDAMENTO — continua possível a qualquer momento, é uma ação
     * separada do encerramento da visita.
     * ADR-0015: precisa de transação própria pra que o evento publicado no
     * fim só seja entregue na fila depois que este método inteiro commitar
     * (ver SeriesService.register).
     */
    @Transactional
    public TrainingResponse close(UUID userId, UUID trainingId) {
        Training training = findOwnedTrainingOrThrow(userId, trainingId);

        if (!training.isOpen()) {
            throw new BusinessException("TRAINING_ALREADY_CLOSED", HttpStatus.CONFLICT);
        }

        training.close(Instant.now());

        Training saved = trainingGateway.save(training);
        applicationEventPublisher.publishEvent(new DashboardRecalculationRequestedEvent(userId));
        return toResponse(saved);
    }

    /**
     * UC34/ADR-0012: chamado por VisitService.close() pra encerrar (mesmo
     * timestamp da visita) qualquer treino dela ainda EM_ANDAMENTO.
     */
    public void closeAllOpenByVisitId(UUID visitId, Instant closedAt) {
        trainingGateway.findAllByVisitIdAndStatus(visitId, TrainingStatus.IN_PROGRESS)
                .forEach(training -> {
                    training.close(closedAt);
                    trainingGateway.save(training);
                });
    }

    /**
     * Reaproveitado por VisitService (UC35) pra montar a lista de treinos
     * aninhados de uma visita.
     */
    public List<TrainingResponse> listByVisitId(UUID visitId) {
        return trainingGateway.findAllByVisitId(visitId).stream()
                .map(this::toResponse)
                .toList();
    }

    private Visit findOwnedVisitOrThrow(UUID userId, UUID visitId) {
        return visitGateway.findById(visitId)
                .filter(visit -> visit.getUserId().equals(userId))
                .orElseThrow(() -> new BusinessException("VISIT_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    /**
     * Reaproveitado por SeriesService (UC36/UC37) pra validar posse do
     * treino informado antes de registrar/listar séries.
     */
    public Training findOwnedTrainingOrThrow(UUID userId, UUID trainingId) {
        Training training = trainingGateway.findById(trainingId)
                .orElseThrow(() -> new BusinessException("TRAINING_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!isOwnedByUser(trainingId, userId)) {
            throw new BusinessException("TRAINING_NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        return training;
    }

    /**
     * Reaproveitado por SeriesService (UC38/UC41) pra validar posse de uma
     * série (via treino) sem lançar TRAINING_NOT_FOUND — a série tem seu
     * próprio código (SERIES_NOT_FOUND) quando o treino não é do atleta.
     */
    public boolean isOwnedByUser(UUID trainingId, UUID userId) {
        return trainingGateway.findById(trainingId)
                .flatMap(training -> visitGateway.findById(training.getVisitId()))
                .map(visit -> visit.getUserId().equals(userId))
                .orElse(false);
    }

    private TrainingResponse toResponse(Training training) {
        Modality modality = modalityGateway.findById(training.getModalityId()).orElseThrow();
        return trainingMapper.toResponse(training, modality);
    }
}
