package com.shottrack.backend.application.seriesphoto.model;

import com.shottrack.backend.common.jpa.AbstractBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * ADR-0026: metadados da foto do alvo de uma série. O arquivo (JPEG já processado) e a miniatura ficam no
 * armazenamento, sob as chaves guardadas aqui. O id é gerado antes (a chave do arquivo usa o id da foto).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@Entity
@Table(name = "series_photos")
public class SeriesPhoto extends AbstractBaseEntity {

    @Id
    private final UUID id;

    @Column(name = "series_id", nullable = false, updatable = false)
    private final UUID seriesId;

    @Column(name = "athlete_id", nullable = false, updatable = false)
    private final UUID athleteId;

    @Column(name = "storage_key", nullable = false, updatable = false)
    private final String storageKey;

    @Column(name = "thumb_key", nullable = false, updatable = false)
    private final String thumbKey;

    @Column(nullable = false, updatable = false)
    private final int position;

    @Column(name = "width_px", nullable = false, updatable = false)
    private final int widthPx;

    @Column(name = "height_px", nullable = false, updatable = false)
    private final int heightPx;

    @Column(name = "size_bytes", nullable = false, updatable = false)
    private final long sizeBytes;

    @Builder
    private SeriesPhoto(UUID id, UUID seriesId, UUID athleteId, String storageKey, String thumbKey, int position,
                        int widthPx, int heightPx, long sizeBytes) {
        this.id = id;
        this.seriesId = seriesId;
        this.athleteId = athleteId;
        this.storageKey = storageKey;
        this.thumbKey = thumbKey;
        this.position = position;
        this.widthPx = widthPx;
        this.heightPx = heightPx;
        this.sizeBytes = sizeBytes;
    }
}
