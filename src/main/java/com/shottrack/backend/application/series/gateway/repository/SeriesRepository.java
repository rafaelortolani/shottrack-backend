package com.shottrack.backend.application.series.gateway.repository;

import com.shottrack.backend.application.series.model.Series;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.Collection;

public interface SeriesRepository extends JpaRepository<Series, UUID> {

    List<Series> findAllByTrainingId(UUID trainingId);

    boolean existsByWeaponId(UUID weaponId);

    boolean existsByAmmunitionId(UUID ammunitionId);

    @Query("select coalesce(sum(s.shotCount), 0) from Series s, Training t, Visit v where t.id = s.trainingId"
            + " and v.id = t.visitId and v.userId = :userId and t.createdAt >= :from and t.createdAt < :to")
    long sumShotCountByUserIdBetween(@Param("userId") UUID userId, @Param("from") Instant from, @Param("to") Instant to);

    @Query("select coalesce(sum(s.shotCount), 0) from Series s, Training t, Visit v where t.id = s.trainingId"
            + " and v.id = t.visitId and v.userId = :userId and t.modalityId = :modalityId"
            + " and t.createdAt >= :from and t.createdAt < :to")
    long sumShotCountByUserIdAndModalityIdBetween(@Param("userId") UUID userId, @Param("modalityId") UUID modalityId,
                                                  @Param("from") Instant from, @Param("to") Instant to);

    List<Series> findAllByTrainingIdIn(Collection<UUID> trainingIds);
    void deleteAllByTrainingIdIn(Collection<UUID> trainingIds);
}
