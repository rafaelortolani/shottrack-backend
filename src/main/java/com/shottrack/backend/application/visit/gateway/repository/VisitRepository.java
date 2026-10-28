package com.shottrack.backend.application.visit.gateway.repository;

import com.shottrack.backend.application.visit.gateway.ClosedVisitFact;
import com.shottrack.backend.application.visit.model.Visit;
import com.shottrack.backend.application.visit.model.VisitType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

import java.util.List;
import java.util.UUID;

public interface VisitRepository extends JpaRepository<Visit, UUID> {

    List<Visit> findAllByUserId(UUID userId);

    boolean existsByTrainingLocationId(UUID trainingLocationId);

    void deleteAllByUserId(UUID userId);

    @Query("select new com.shottrack.backend.application.visit.gateway.ClosedVisitFact("
            + "v.visitType, v.competitionScope, l.club, v.trainingLocationId, v.startedAt) "
            + "from Visit v, TrainingLocation l where l.id = v.trainingLocationId and v.userId = :userId "
            + "and v.status = com.shottrack.backend.application.visit.model.VisitStatus.CLOSED "
            + "and v.startedAt >= :from and v.startedAt < :to")
    List<ClosedVisitFact> findClosedFactsByUserIdBetween(@Param("userId") UUID userId, @Param("from") Instant from,
                                                         @Param("to") Instant to);

    @Query("select count(v) from Visit v where v.userId = :userId and v.visitType = :type "
            + "and v.status = com.shottrack.backend.application.visit.model.VisitStatus.CLOSED "
            + "and v.startedAt >= :from and v.startedAt < :to")
    long countClosedByUserIdAndTypeBetween(@Param("userId") UUID userId, @Param("type") VisitType type,
                                           @Param("from") Instant from, @Param("to") Instant to);
}
