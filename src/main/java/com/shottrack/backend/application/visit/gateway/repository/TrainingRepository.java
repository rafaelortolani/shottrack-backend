package com.shottrack.backend.application.visit.gateway.repository;

import com.shottrack.backend.application.visit.model.Training;
import com.shottrack.backend.application.visit.model.TrainingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TrainingRepository extends JpaRepository<Training, UUID> {

    List<Training> findAllByVisitId(UUID visitId);

    List<Training> findAllByVisitIdAndStatus(UUID visitId, TrainingStatus status);

    boolean existsByVisitIdInAndModalityId(Collection<UUID> visitIds, UUID modalityId);

    @Query("select count(t) from Training t, Visit v where v.id = t.visitId and v.userId = :userId"
            + " and t.createdAt >= :from and t.createdAt < :to")
    long countByUserIdBetween(@Param("userId") UUID userId, @Param("from") Instant from, @Param("to") Instant to);

    @Query("select count(t) from Training t, Visit v where v.id = t.visitId and v.userId = :userId"
            + " and t.modalityId = :modalityId and t.createdAt >= :from and t.createdAt < :to")
    long countByUserIdAndModalityIdBetween(@Param("userId") UUID userId, @Param("modalityId") UUID modalityId,
                                           @Param("from") Instant from, @Param("to") Instant to);

    @Query("select count(distinct t.modalityId) from Training t, Visit v where v.id = t.visitId"
            + " and v.userId = :userId and t.createdAt >= :from and t.createdAt < :to")
    long countDistinctModalitiesByUserIdBetween(@Param("userId") UUID userId, @Param("from") Instant from,
                                                @Param("to") Instant to);

    List<Training> findAllByVisitIdIn(Collection<UUID> visitIds);
    void deleteAllByVisitIdIn(Collection<UUID> visitIds);
}
