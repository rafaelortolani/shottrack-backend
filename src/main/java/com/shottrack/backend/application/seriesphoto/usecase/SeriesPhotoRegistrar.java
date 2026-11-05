package com.shottrack.backend.application.seriesphoto.usecase;

import com.shottrack.backend.application.plan.model.PlanLimit;
import com.shottrack.backend.application.plan.usecase.PlanLimitService;
import com.shottrack.backend.application.seriesphoto.gateway.SeriesPhotoGateway;
import com.shottrack.backend.application.seriesphoto.model.SeriesPhoto;
import com.shottrack.backend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * UC60/ADR-0027: confere os limites do plano E grava a linha da foto na MESMA transação, sob um lock por atleta.
 * Sem isso dois envios simultâneos leriam a mesma contagem e os dois passariam do limite. O lock é consultivo
 * (pg_advisory_xact_lock) e só segura quem envia foto do mesmo atleta; some no commit/rollback.
 * Bean à parte do SeriesPhotoService para que o @Transactional valha (o service não é transacional de propósito:
 * o processamento da imagem não segura conexão).
 */
@Component
@RequiredArgsConstructor
public class SeriesPhotoRegistrar {

    private final SeriesPhotoGateway photoGateway;
    private final PlanLimitService planLimitService;

    /** A foto chega com `sizeBytes` da imagem já processada (a miniatura não conta). */
    @Transactional
    public SeriesPhoto register(SeriesPhoto.SeriesPhotoBuilder photo, UUID athleteId, UUID seriesId, long sizeBytes) {
        photoGateway.lockAthleteUploads(athleteId);
        Map<PlanLimit, Long> limits = planLimitService.limitsOf(athleteId);

        if (photoGateway.countBySeriesId(seriesId) >= limits.get(PlanLimit.PHOTOS_PER_SERIES)) {
            throw new BusinessException("PHOTO_LIMIT_PER_SERIES_REACHED", HttpStatus.CONFLICT);
        }
        SeriesPhotoGateway.Usage usage = photoGateway.usageOf(athleteId);
        if (usage.photos() >= limits.get(PlanLimit.PHOTOS_PER_ATHLETE)) {
            throw new BusinessException("PHOTO_LIMIT_PER_ATHLETE_REACHED", HttpStatus.CONFLICT);
        }
        if (usage.storageBytes() + sizeBytes > limits.get(PlanLimit.STORAGE_BYTES_PER_ATHLETE)) {
            throw new BusinessException("PHOTO_STORAGE_LIMIT_REACHED", HttpStatus.CONFLICT);
        }
        return photoGateway.save(photo.position(photoGateway.nextPosition(seriesId)).sizeBytes(sizeBytes).build());
    }
}
