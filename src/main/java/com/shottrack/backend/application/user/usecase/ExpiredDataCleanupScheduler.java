package com.shottrack.backend.application.user.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * ADR-0019: dispara a limpeza todo dia. Desligado por app.cleanup.enabled=false
 * (testes) — a regra em si mora em ExpiredDataCleanupService.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.cleanup.enabled", havingValue = "true")
public class ExpiredDataCleanupScheduler {

    private final ExpiredDataCleanupService expiredDataCleanupService;

    @Scheduled(cron = "${app.cleanup.cron}", zone = "UTC")
    public void run() {
        expiredDataCleanupService.cleanup();
    }
}
