package com.shottrack.backend.application.seriesphoto.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * ADR-0026: onde ficam os arquivos e como a rotina de órfãos se comporta. `requireExistingDir` (ligado no perfil prod) faz o app falhar na subida se o
 * diretório não existir ou não for gravável pelo usuário do contêiner, em vez de falhar no primeiro upload.
 */
@ConfigurationProperties("app.storage")
public record StorageProperties(
        @DefaultValue("./data/uploads") String localDir,
        @DefaultValue("false") boolean requireExistingDir,
        @DefaultValue OrphanCleanup orphanCleanup
) {

    /**
     * `enabled`: só o perfil prod liga. `dryRun`: só registra quantos arquivos apagaria. A rotina aborta, sem
     * apagar nada, se achar mais de max(abortMinCandidates, abortPercent% dos arquivos de foto) candidatos a órfão.
     */
    public record OrphanCleanup(
            @DefaultValue("false") boolean enabled,
            @DefaultValue("false") boolean dryRun,
            @DefaultValue("20") int abortMinCandidates,
            @DefaultValue("10") int abortPercent
    ) {
    }
}
