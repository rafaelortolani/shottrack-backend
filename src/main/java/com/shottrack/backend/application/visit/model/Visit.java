package com.shottrack.backend.application.visit.model;

import com.shottrack.backend.common.jpa.AbstractBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Visita (ADR-0012): permanência do atleta num local de treino (ADR-0010),
 * dentro da qual ele pode abrir vários treinos. iniciada_em é o próprio
 * started_at (ADR-0024): data de início da visita — pode ser retroativa e editada; é a data
 * de referência de todas as apurações. O createdAt de auditoria é quando o registro foi lançado.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@Entity
@Table(name = "visits")
public class Visit extends AbstractBaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private final UUID userId;

    @Column(name = "training_location_id", nullable = false)
    private final UUID trainingLocationId;

    @Setter
    @Column
    private String observations;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VisitStatus status = VisitStatus.IN_PROGRESS;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    /** ADR-0023: padrão TREINO; o âmbito só existe para COMPETICAO. */
    @Enumerated(EnumType.STRING)
    @Column(name = "visit_type", nullable = false)
    private VisitType visitType = VisitType.TREINO;

    @Enumerated(EnumType.STRING)
    @Column(name = "competition_scope")
    private CompetitionScope competitionScope;

    @Builder
    private Visit(UUID userId, UUID trainingLocationId, String observations, VisitType visitType,
                  CompetitionScope competitionScope, Instant startedAt) {
        this.userId = userId;
        this.trainingLocationId = trainingLocationId;
        this.observations = observations;
        if (visitType != null) {
            this.visitType = visitType;
        }
        this.competitionScope = competitionScope;
        this.startedAt = startedAt != null ? startedAt : Instant.now();
    }

    /**
     * UC34/ADR-0012: status e encerrada_em nunca entram no builder — só
     * mudam por aqui, pra não permitir criar uma visita já "pré-encerrada".
     */
    public void close(Instant closedAt) {
        this.status = VisitStatus.CLOSED;
        this.endedAt = closedAt;
    }

    /**
     * UC59/ADR-0024: edição das datas; null = não alterar. A validação (futuro, 12 meses,
     * ordem, visita em andamento) é do usecase.
     */
    public void changeDates(Instant newStartedAt, Instant newEndedAt) {
        if (newStartedAt != null) {
            this.startedAt = newStartedAt;
        }
        if (newEndedAt != null) {
            this.endedAt = newEndedAt;
        }
    }

    public boolean isOpen() {
        return status == VisitStatus.IN_PROGRESS;
    }
}
