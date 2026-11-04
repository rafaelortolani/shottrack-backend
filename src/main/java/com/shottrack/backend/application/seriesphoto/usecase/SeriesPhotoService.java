package com.shottrack.backend.application.seriesphoto.usecase;

import com.shottrack.backend.application.series.model.Series;
import com.shottrack.backend.application.series.usecase.SeriesService;
import com.shottrack.backend.application.seriesphoto.PhotoProperties;
import com.shottrack.backend.application.seriesphoto.dto.SeriesPhotoResponse;
import com.shottrack.backend.application.seriesphoto.gateway.SeriesPhotoGateway;
import com.shottrack.backend.application.seriesphoto.image.ImageProcessor;
import com.shottrack.backend.application.seriesphoto.image.ProcessedImage;
import com.shottrack.backend.application.seriesphoto.model.SeriesPhoto;
import com.shottrack.backend.application.seriesphoto.storage.StoragePort;
import com.shottrack.backend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * UC60/UC61/UC62 (ADR-0026): fotos do alvo da série. Nunca loga conteúdo, nome de arquivo nem caminho.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeriesPhotoService {

    private final SeriesService seriesService;
    private final SeriesPhotoGateway photoGateway;
    private final StoragePort storage;
    private final ImageProcessor imageProcessor;
    private final SeriesPhotoFiles photoFiles;
    private final PhotoProperties properties;

    /**
     * Sem @Transactional de propósito: o processamento da imagem não segura conexão do banco. Ordem (UC60):
     * arquivos primeiro, depois a linha; se a linha falhar, os arquivos são apagados; se um arquivo falhar,
     * não há linha.
     */
    public SeriesPhotoResponse attach(UUID userId, UUID seriesId, MultipartFile file) {
        Series series = seriesService.findOwnedSeriesOrThrow(userId, seriesId);

        if (file == null || file.isEmpty()) {
            throw new BusinessException("VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > properties.maxBytes()) {
            throw new BusinessException("PHOTO_TOO_LARGE", HttpStatus.PAYLOAD_TOO_LARGE);
        }
        if (photoGateway.countBySeriesId(series.getId()) >= properties.maxPerSeries()) {
            throw new BusinessException("PHOTO_LIMIT_PER_SERIES_REACHED", HttpStatus.CONFLICT);
        }
        if (photoGateway.countByAthleteId(userId) >= properties.maxPerAthlete()) {
            throw new BusinessException("PHOTO_LIMIT_PER_ATHLETE_REACHED", HttpStatus.CONFLICT);
        }

        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException("PHOTO_INVALID_IMAGE", HttpStatus.BAD_REQUEST);
        }
        ProcessedImage processed = imageProcessor.process(data);

        UUID photoId = UUID.randomUUID();
        String base = "athletes/" + userId + "/series/" + series.getId() + "/" + photoId;
        String storageKey = base + ".jpg";
        String thumbKey = base + ".thumb.jpg";

        try {
            storage.put(storageKey, processed.image());
            storage.put(thumbKey, processed.thumbnail());
        } catch (RuntimeException e) {
            photoFiles.deleteQuietly(List.of(storageKey, thumbKey));
            throw e;
        }

        try {
            SeriesPhoto saved = photoGateway.save(SeriesPhoto.builder()
                    .id(photoId)
                    .seriesId(series.getId())
                    .athleteId(userId)
                    .storageKey(storageKey)
                    .thumbKey(thumbKey)
                    .position(photoGateway.nextPosition(series.getId()))
                    .widthPx(processed.widthPx())
                    .heightPx(processed.heightPx())
                    .sizeBytes(processed.image().length)
                    .build());
            return toResponse(saved);
        } catch (RuntimeException e) {
            photoFiles.deleteQuietly(List.of(storageKey, thumbKey));
            throw e;
        }
    }

    public List<SeriesPhotoResponse> list(UUID userId, UUID seriesId) {
        Series series = seriesService.findOwnedSeriesOrThrow(userId, seriesId);
        return photoGateway.findAllBySeriesId(series.getId()).stream().map(SeriesPhotoService::toResponse).toList();
    }

    public byte[] content(UUID userId, UUID seriesId, UUID photoId, boolean thumbnail) {
        SeriesPhoto photo = findOwnedPhotoOrThrow(userId, seriesId, photoId);
        return storage.get(thumbnail ? photo.getThumbKey() : photo.getStorageKey()).orElseThrow(() -> {
            log.error("Arquivo de foto ausente do armazenamento");
            return new BusinessException("PHOTO_NOT_FOUND", HttpStatus.NOT_FOUND);
        });
    }

    /** UC62: remove a linha; os arquivos só depois do commit. */
    @Transactional
    public void delete(UUID userId, UUID seriesId, UUID photoId) {
        SeriesPhoto photo = findOwnedPhotoOrThrow(userId, seriesId, photoId);
        photoGateway.delete(photo);
        photoFiles.deleteAfterCommit(List.of(photo.getStorageKey(), photo.getThumbKey()));
    }

    /** A série é do atleta E a foto pertence a ESSA série (photoId de outra série → PHOTO_NOT_FOUND). */
    private SeriesPhoto findOwnedPhotoOrThrow(UUID userId, UUID seriesId, UUID photoId) {
        Series series = seriesService.findOwnedSeriesOrThrow(userId, seriesId);
        return photoGateway.findById(photoId)
                .filter(photo -> photo.getSeriesId().equals(series.getId()) && photo.getAthleteId().equals(userId))
                .orElseThrow(() -> new BusinessException("PHOTO_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    private static SeriesPhotoResponse toResponse(SeriesPhoto photo) {
        return new SeriesPhotoResponse(photo.getId(), photo.getPosition(), photo.getWidthPx(), photo.getHeightPx(),
                photo.getSizeBytes(), photo.getCreatedAt());
    }
}
