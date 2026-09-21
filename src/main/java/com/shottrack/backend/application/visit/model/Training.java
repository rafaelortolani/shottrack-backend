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

import java.time.Instant;
import java.util.UUID;

/**
 * Treino (ADR-0012): pertence a uma Visita em andamento e a uma Modalidade
 * praticada pelo atleta (UC12). Vários treinos podem estar EM_ANDAMENTO ao
 * mesmo tempo na mesma visita, inclusive repetindo a mesma modalidade — sem
 * restrição de unicidade. iniciado_em é o próprio createdAt de auditoria —
 * não duplicado aqui como campo à parte.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@Entity
@Table(name = "trainings")
public class Training extends AbstractBaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "visit_id", nullable = false)
    private final UUID visitId;

    @Column(name = "modality_id", nullable = false)
    private final UUID modalityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrainingStatus status = TrainingStatus.IN_PROGRESS;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Builder
    private Training(UUID visitId, UUID modalityId) {
        this.visitId = visitId;
        this.modalityId = modalityId;
    }

    /**
     * UC33/UC34/ADR-0012: status e encerrado_em nunca entram no builder — só
     * mudam por aqui, pra não permitir criar um treino já "pré-encerrado".
     */
    public void close(Instant closedAt) {
        this.status = TrainingStatus.CLOSED;
        this.endedAt = closedAt;
    }

    public boolean isOpen() {
        return status == TrainingStatus.IN_PROGRESS;
    }
}
