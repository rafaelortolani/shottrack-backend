package com.shottrack.backend.application.seriesphoto.dto;

import java.time.Instant;
import java.util.UUID;

/** UC60/UC61/UC53: metadados da foto — nunca o caminho nem a chave do arquivo. */
public record SeriesPhotoResponse(
        UUID id,
        int position,
        int widthPx,
        int heightPx,
        long sizeBytes,
        Instant createdAt
) {
}
