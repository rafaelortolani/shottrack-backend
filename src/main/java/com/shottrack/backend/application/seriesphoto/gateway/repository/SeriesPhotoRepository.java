package com.shottrack.backend.application.seriesphoto.gateway.repository;

import com.shottrack.backend.application.seriesphoto.model.SeriesPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SeriesPhotoRepository extends JpaRepository<SeriesPhoto, UUID> {

    List<SeriesPhoto> findAllBySeriesIdOrderByPositionAscCreatedAtAsc(UUID seriesId);

    List<SeriesPhoto> findAllBySeriesIdIn(Collection<UUID> seriesIds);

    List<SeriesPhoto> findAllByAthleteId(UUID athleteId);

    long countBySeriesId(UUID seriesId);

    long countByAthleteId(UUID athleteId);

    @Query("select coalesce(max(p.position), 0) from SeriesPhoto p where p.seriesId = :seriesId")
    int maxPositionBySeriesId(@Param("seriesId") UUID seriesId);

    /** UC37: contagem agrupada — uma consulta para todas as séries da lista (sem N+1). */
    @Query("select p.seriesId, count(p) from SeriesPhoto p where p.seriesId in :seriesIds group by p.seriesId")
    List<Object[]> countGroupedBySeriesIdIn(@Param("seriesIds") Collection<UUID> seriesIds);

    @Query("select p from SeriesPhoto p where p.seriesId in (select s.id from Series s where s.trainingId = :trainingId)")
    List<SeriesPhoto> findAllByTrainingId(@Param("trainingId") UUID trainingId);

    @Query("select p from SeriesPhoto p where p.seriesId in (select s.id from Series s, Training t"
            + " where s.trainingId = t.id and t.visitId = :visitId)")
    List<SeriesPhoto> findAllByVisitId(@Param("visitId") UUID visitId);

    @Query("select count(p) > 0 from SeriesPhoto p where p.storageKey = :key or p.thumbKey = :key")
    boolean existsByAnyKey(@Param("key") String key);

    @Modifying
    @Query("delete from SeriesPhoto p where p.athleteId = :athleteId")
    void deleteAllByAthleteId(@Param("athleteId") UUID athleteId);
}
