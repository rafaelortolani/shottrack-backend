package com.shottrack.backend.application.visit.usecase;

import com.shottrack.backend.application.dashboard.event.DashboardRecalculationRequestedEvent;
import com.shottrack.backend.application.traininglocation.usecase.TrainingLocationService;
import com.shottrack.backend.application.visit.dto.EditVisitDatesRequest;
import com.shottrack.backend.application.visit.dto.StartVisitRequest;
import com.shottrack.backend.application.visit.dto.TrainingResponse;
import com.shottrack.backend.application.visit.dto.VisitResponse;
import com.shottrack.backend.application.visit.gateway.VisitGateway;
import com.shottrack.backend.application.visit.mapper.VisitMapper;
import com.shottrack.backend.application.visit.model.Visit;
import com.shottrack.backend.application.visit.model.VisitType;
import com.shottrack.backend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VisitService {

    /**
     * ADR-0024: tolerância para relógio de cliente adiantado — uma data até 5 minutos à frente
     * é aceita e tratada como "agora"; além disso é futuro (VISIT_DATE_IN_FUTURE).
     */
    static final Duration FUTURE_TOLERANCE = Duration.ofMinutes(5);
    static final int MAX_MONTHS_BACK = 12;

    private final VisitGateway visitGateway;
    private final TrainingLocationService trainingLocationService;
    private final TrainingService trainingService;
    private final VisitMapper visitMapper;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Clock clock;

    /**
     * UC31/ADR-0024: startedAt opcional (padrão agora). Transação própria porque a visita
     * retroativa dispara o recálculo do dashboard, entregue só depois do commit (ADR-0015).
     */
    @Transactional
    public VisitResponse start(UUID userId, StartVisitRequest request) {
        trainingLocationService.findOwnedTrainingLocationOrThrow(userId, request.trainingLocationId());

        // ADR-0023: âmbito só faz sentido numa competição (visitType ausente = TREINO).
        if (request.competitionScope() != null && request.visitType() != VisitType.COMPETICAO) {
            throw new BusinessException("VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }

        Visit visit = Visit.builder()
                .userId(userId)
                .trainingLocationId(request.trainingLocationId())
                .observations(request.observations())
                .visitType(request.visitType())
                .competitionScope(request.competitionScope())
                .startedAt(request.startedAt() == null ? null : validatedStart(request.startedAt()))
                .build();

        Visit saved = visitGateway.save(visit);
        if (request.startedAt() != null) {
            applicationEventPublisher.publishEvent(new DashboardRecalculationRequestedEvent(userId));
        }
        return toResponse(saved);
    }

    /**
     * UC59/ADR-0024: edita início e/ou fim. Ordem das validações: corpo vazio, fim em visita
     * em andamento, futuro, 12 meses, ordem início ≤ fim. Os horários dos treinos e séries
     * não mudam. Recalcula o dashboard depois do commit.
     */
    @Transactional
    public VisitResponse editDates(UUID userId, UUID visitId, EditVisitDatesRequest request) {
        Visit visit = findOwnedVisitOrThrow(userId, visitId);

        if (request.startedAt() == null && request.endedAt() == null) {
            throw new BusinessException("VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }
        if (request.endedAt() != null && visit.isOpen()) {
            throw new BusinessException("VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }

        Instant newStart = request.startedAt() == null ? null : validatedStart(request.startedAt());
        Instant newEnd = request.endedAt() == null ? null : validatedEnd(request.endedAt());

        Instant start = newStart != null ? newStart : visit.getStartedAt();
        Instant end = newEnd != null ? newEnd : visit.getEndedAt();
        if (end != null && start.isAfter(end)) {
            throw new BusinessException("VISIT_DATE_RANGE_INVALID", HttpStatus.BAD_REQUEST);
        }

        visit.changeDates(newStart, newEnd);
        Visit saved = visitGateway.save(visit);
        applicationEventPublisher.publishEvent(new DashboardRecalculationRequestedEvent(userId));
        return toResponse(saved);
    }

    /**
     * Não futuro (com a tolerância de relógio) e não anterior a 12 meses. O limite é por dia
     * (UTC), como as demais janelas: vale a partir das 00:00 UTC do dia de hoje menos 12 meses.
     * Um valor ligeiramente à frente do relógio é ajustado para "agora", para nunca ficar
     * depois do encerramento.
     */
    private Instant validatedStart(Instant startedAt) {
        Instant now = clock.instant();
        Instant effective = notInFuture(startedAt, now);
        LocalDate oldestDay = LocalDate.ofInstant(now, ZoneOffset.UTC).minusMonths(MAX_MONTHS_BACK);
        if (effective.isBefore(oldestDay.atStartOfDay().toInstant(ZoneOffset.UTC))) {
            throw new BusinessException("VISIT_DATE_TOO_OLD", HttpStatus.BAD_REQUEST);
        }
        return effective;
    }

    private Instant validatedEnd(Instant endedAt) {
        return notInFuture(endedAt, clock.instant());
    }

    private Instant notInFuture(Instant value, Instant now) {
        if (value.isAfter(now.plus(FUTURE_TOLERANCE))) {
            throw new BusinessException("VISIT_DATE_IN_FUTURE", HttpStatus.BAD_REQUEST);
        }
        return value.isAfter(now) ? now : value;
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

        Instant closedAt = clock.instant();
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
