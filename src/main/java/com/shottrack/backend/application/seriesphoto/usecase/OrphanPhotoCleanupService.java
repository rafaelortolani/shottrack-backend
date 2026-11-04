package com.shottrack.backend.application.seriesphoto.usecase;

import com.shottrack.backend.application.seriesphoto.PhotoProperties;
import com.shottrack.backend.application.seriesphoto.gateway.SeriesPhotoGateway;
import com.shottrack.backend.application.seriesphoto.storage.StoragePort;
import com.shottrack.backend.application.seriesphoto.storage.StoredObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

/**
 * ADR-0026: remove do armazenamento os arquivos sem linha no banco (falha ao apagar após um commit, falha entre
 * gravar o arquivo e a linha). Arquivo criado há menos de `app.photos.orphan-min-age` (60 min) nunca é tocado: pode
 * ser um upload em andamento (os arquivos são gravados antes da linha). Loga só o total.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrphanPhotoCleanupService {

    private final StoragePort storage;
    private final SeriesPhotoGateway photoGateway;
    private final PhotoProperties properties;
    private final Clock clock;

    public int cleanup() {
        Instant limit = clock.instant().minus(properties.orphanMinAge());
        int removed = 0;
        for (StoredObject object : storage.list()) {
            if (object.lastModified().isAfter(limit) || photoGateway.existsByAnyKey(object.key())) {
                continue;
            }
            try {
                storage.delete(object.key());
                removed++;
            } catch (RuntimeException e) {
                log.warn("Falha ao apagar arquivo de foto órfão ({})", e.getClass().getSimpleName());
            }
        }
        log.info("Limpeza de fotos órfãs: {} arquivo(s) removido(s)", removed);
        return removed;
    }
}
