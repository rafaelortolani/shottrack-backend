package com.shottrack.backend.application.seriesphoto.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * ADR-0026: dispara a limpeza de fotos órfãs toda semana. Desligado por app.storage.orphan-cleanup.enabled (padrão false; só o perfil prod liga) — a
 * regra mora em OrphanPhotoCleanupService.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.storage.orphan-cleanup.enabled", havingValue = "true")
public class OrphanPhotoCleanupScheduler {

    private final OrphanPhotoCleanupService orphanPhotoCleanupService;

    @Scheduled(cron = "${app.photos.orphan-cleanup-cron}", zone = "UTC")
    public void run() {
        orphanPhotoCleanupService.cleanup();
    }
}
