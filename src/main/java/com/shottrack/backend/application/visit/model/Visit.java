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
 * createdAt de auditoria — não duplicado aqui como campo à parte.
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

    @Column(name = "ended_at")
    private Instant endedAt;

    @Builder
    private Visit(UUID userId, UUID trainingLocationId, String observations) {
        this.userId = userId;
        this.trainingLocationId = trainingLocationId;
        this.observations = observations;
    }

    /**
     * UC34/ADR-0012: status e encerrada_em nunca entram no builder — só
     * mudam por aqui, pra não permitir criar uma visita já "pré-encerrada".
     */
    public void close(Instant closedAt) {
        this.status = VisitStatus.CLOSED;
        this.endedAt = closedAt;
    }

    public boolean isOpen() {
        return status == VisitStatus.IN_PROGRESS;
    }
}
